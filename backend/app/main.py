import secrets
import string
from collections import Counter
from contextlib import asynccontextmanager
from fastapi import Depends, FastAPI, HTTPException, Query, status
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import and_, select
from sqlalchemy.orm import Session

from .config import settings
from .db import Base, SessionLocal, engine, get_db
from .models import (
    Attendance, ClassStudent, DirectorNotice, Grade, Institution, Notice,
    ParticipationReport, ParticipationReview, Role, ScheduleEntry, SchoolClass,
    TeamActivity, User,
)
from .schemas import (
    AttendanceIn, ClassIn, GradeIn, InstitutionIn, JoinClassIn, LoginIn, NoticeIn,
    ParticipationReportIn, ParticipationReviewIn, RegisterIn, ScheduleIn,
    TeamActivityIn, TeamScoresIn, TokenOut,
)
from .security import create_access_token, current_user, hash_password, require_roles, verify_password


def user_dict(user: User):
    return {"id": user.id, "email": user.email, "full_name": user.full_name, "role": user.role.value, "institution_id": user.institution_id}


def class_dict(item: SchoolClass):
    return {"id": item.id, "name": item.name, "subject": item.subject, "period_name": item.period_name, "class_code": item.class_code, "teacher_id": item.teacher_id, "institution_id": item.institution_id, "active": item.active}


def notice_dict(item: DirectorNotice):
    return {"id": item.id, "title": item.title, "body": item.body, "created_at": item.created_at, "updated_at": item.updated_at}


def get_class_or_404(db: Session, class_id: int) -> SchoolClass:
    item = db.get(SchoolClass, class_id)
    if not item:
        raise HTTPException(404, "Class not found")
    return item


def teacher_owns(user: User, item: SchoolClass):
    if user.role != Role.TEACHER or item.teacher_id != user.id:
        raise HTTPException(403, "Not allowed for this class")


def student_member(db: Session, user: User, class_id: int):
    found = db.scalar(select(ClassStudent).where(ClassStudent.class_id == class_id, ClassStudent.student_id == user.id))
    if not found:
        raise HTTPException(403, "Student is not enrolled in this class")


def class_access(db: Session, user: User, item: SchoolClass):
    if user.role == Role.TEACHER and item.teacher_id == user.id:
        return
    if user.role == Role.STUDENT:
        return student_member(db, user, item.id)
    if user.role == Role.DIRECTOR and user.institution_id and user.institution_id == item.institution_id:
        return
    raise HTTPException(403, "Not allowed for this class")


def make_code(db: Session) -> str:
    alphabet = string.ascii_uppercase + string.digits
    for _ in range(20):
        code = "".join(secrets.choice(alphabet) for _ in range(6))
        if not db.scalar(select(SchoolClass).where(SchoolClass.class_code == code)):
            return code
    raise HTTPException(500, "Could not generate class code")


def same_team(activity: TeamActivity, a: int, b: int) -> bool:
    for team in activity.teams or []:
        ids = [int(x) for x in team.get("student_ids", [])]
        if a in ids and b in ids:
            return True
    return False


def consolidate_team_scores(db: Session, activity: TeamActivity):
    for team in activity.teams or []:
        team_name = team.get("name", "")
        base = (activity.base_scores or {}).get(team_name)
        for student_id in team.get("student_ids", []):
            override = (activity.individual_scores or {}).get(str(student_id))
            score = override if override is not None else base
            if score is None:
                continue
            existing = db.scalar(select(Grade).where(
                Grade.class_id == activity.class_id,
                Grade.student_id == int(student_id),
                Grade.category == activity.category,
                Grade.activity_key == activity.activity_key,
            ))
            if existing:
                existing.score = float(score)
                existing.max_score = 100.0
                existing.activity_name = activity.name
                existing.source = "team_activity"
            else:
                db.add(Grade(class_id=activity.class_id, student_id=int(student_id), category=activity.category,
                             activity_key=activity.activity_key, activity_name=activity.name, score=float(score),
                             max_score=100.0, source="team_activity"))


def bootstrap_director():
    if not settings.bootstrap_director_email or not settings.bootstrap_director_password:
        return
    with SessionLocal() as db:
        email = settings.bootstrap_director_email.strip().lower()
        if db.scalar(select(User).where(User.email == email)):
            return
        db.add(User(email=email, password_hash=hash_password(settings.bootstrap_director_password), full_name="Director", role=Role.DIRECTOR))
        db.commit()


@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(engine)
    bootstrap_director()
    yield


app = FastAPI(title="ProfeCuaderno Central Backend", version="1.0.0", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=settings.cors_list, allow_credentials=False, allow_methods=["*"], allow_headers=["*"])


@app.get("/health")
def health():
    return {"status": "ok", "service": "profecuaderno-central", "version": "1.0.0"}


@app.post("/auth/register", response_model=TokenOut)
def register(payload: RegisterIn, db: Session = Depends(get_db)):
    email = payload.email.lower()
    if db.scalar(select(User).where(User.email == email)):
        raise HTTPException(409, "Email already registered")
    role = Role(payload.role)
    user = User(email=email, password_hash=hash_password(payload.password), full_name=payload.full_name.strip(), role=role)
    db.add(user); db.commit(); db.refresh(user)
    return TokenOut(access_token=create_access_token(user), user=user_dict(user))


@app.post("/auth/login", response_model=TokenOut)
def login(payload: LoginIn, db: Session = Depends(get_db)):
    user = db.scalar(select(User).where(User.email == payload.email.lower()))
    if not user or not verify_password(payload.password, user.password_hash):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid credentials")
    return TokenOut(access_token=create_access_token(user), user=user_dict(user))


@app.get("/me")
def me(user: User = Depends(current_user)):
    return user_dict(user)


@app.get("/institution")
def current_institution(db: Session = Depends(get_db), user: User = Depends(current_user)):
    if not user.institution_id:
        raise HTTPException(404, "User has no institution")
    institution = db.get(Institution, user.institution_id)
    if not institution:
        raise HTTPException(404, "Institution not found")
    return {"id": institution.id, "name": institution.name}


@app.post("/institutions")
def create_institution(payload: InstitutionIn, db: Session = Depends(get_db), user: User = Depends(require_roles("director"))):
    if user.institution_id:
        raise HTTPException(409, "Director already belongs to an institution")
    institution = Institution(name=payload.name.strip(), created_by=user.id)
    db.add(institution); db.flush(); user.institution_id = institution.id; db.commit(); db.refresh(institution)
    return {"id": institution.id, "name": institution.name}


@app.post("/institutions/attach-teacher")
def attach_teacher(email: str = Query(...), db: Session = Depends(get_db), user: User = Depends(require_roles("director"))):
    if not user.institution_id:
        raise HTTPException(400, "Director has no institution")
    teacher = db.scalar(select(User).where(User.email == email.lower(), User.role == Role.TEACHER))
    if not teacher:
        raise HTTPException(404, "Teacher not found")
    if teacher.institution_id and teacher.institution_id != user.institution_id:
        raise HTTPException(409, "Teacher already belongs to another institution")

    teacher.institution_id = user.institution_id
    teacher_classes = db.scalars(select(SchoolClass).where(SchoolClass.teacher_id == teacher.id)).all()
    class_ids = []
    for item in teacher_classes:
        if item.institution_id is None:
            item.institution_id = user.institution_id
        if item.institution_id == user.institution_id:
            class_ids.append(item.id)

    if class_ids:
        student_ids = db.scalars(select(ClassStudent.student_id).where(ClassStudent.class_id.in_(class_ids))).all()
        students = db.scalars(select(User).where(User.id.in_(student_ids), User.role == Role.STUDENT)).all() if student_ids else []
        for student in students:
            if student.institution_id is None:
                student.institution_id = user.institution_id

    db.commit(); db.refresh(teacher)
    return user_dict(teacher)


@app.get("/institutions/teachers")
def institution_teachers(db: Session = Depends(get_db), user: User = Depends(require_roles("director"))):
    if not user.institution_id:
        return []
    teachers = db.scalars(select(User).where(User.role == Role.TEACHER, User.institution_id == user.institution_id, User.active.is_(True)).order_by(User.full_name, User.email)).all()
    return [user_dict(x) for x in teachers]


@app.post("/institutions/notices")
def create_director_notice(payload: NoticeIn, db: Session = Depends(get_db), user: User = Depends(require_roles("director"))):
    if not user.institution_id:
        raise HTTPException(400, "Director has no institution")
    notice = DirectorNotice(institution_id=user.institution_id, author_id=user.id, title=payload.title.strip(), body=payload.body.strip())
    db.add(notice); db.commit(); db.refresh(notice)
    return notice_dict(notice)


@app.get("/institutions/notices")
def list_director_notices(db: Session = Depends(get_db), user: User = Depends(require_roles("director"))):
    if not user.institution_id:
        return []
    rows = db.scalars(select(DirectorNotice).where(DirectorNotice.institution_id == user.institution_id).order_by(DirectorNotice.created_at.desc())).all()
    return [notice_dict(x) for x in rows]


@app.get("/teacher-notices")
def list_teacher_notices(db: Session = Depends(get_db), user: User = Depends(require_roles("teacher"))):
    if not user.institution_id:
        return []
    rows = db.scalars(select(DirectorNotice).where(DirectorNotice.institution_id == user.institution_id).order_by(DirectorNotice.created_at.desc())).all()
    return [notice_dict(x) for x in rows]


@app.post("/classes")
def create_class(payload: ClassIn, db: Session = Depends(get_db), user: User = Depends(require_roles("teacher"))):
    item = SchoolClass(teacher_id=user.id, institution_id=user.institution_id, name=payload.name.strip(), subject=payload.subject.strip(), period_name=payload.period_name.strip(), class_code=make_code(db))
    db.add(item); db.commit(); db.refresh(item)
    return class_dict(item)


@app.post("/classes/join")
def join_class(payload: JoinClassIn, db: Session = Depends(get_db), user: User = Depends(require_roles("student"))):
    item = db.scalar(select(SchoolClass).where(SchoolClass.class_code == payload.class_code.strip().upper(), SchoolClass.active.is_(True)))
    if not item:
        raise HTTPException(404, "Class code not found")
    if item.institution_id:
        if user.institution_id and user.institution_id != item.institution_id:
            raise HTTPException(409, "Class belongs to another institution")
        if user.institution_id is None:
            user.institution_id = item.institution_id
    existing = db.scalar(select(ClassStudent).where(ClassStudent.class_id == item.id, ClassStudent.student_id == user.id))
    if not existing:
        db.add(ClassStudent(class_id=item.id, student_id=user.id))
    db.commit()
    return class_dict(item)


@app.get("/classes")
def list_classes(db: Session = Depends(get_db), user: User = Depends(current_user)):
    if user.role == Role.TEACHER:
        items = db.scalars(select(SchoolClass).where(SchoolClass.teacher_id == user.id)).all()
    elif user.role == Role.STUDENT:
        ids = db.scalars(select(ClassStudent.class_id).where(ClassStudent.student_id == user.id)).all()
        items = db.scalars(select(SchoolClass).where(SchoolClass.id.in_(ids))).all() if ids else []
    else:
        items = db.scalars(select(SchoolClass).where(SchoolClass.institution_id == user.institution_id)).all() if user.institution_id else []
    return [class_dict(x) for x in items]


@app.get("/classes/{class_id}/students")
def class_students(class_id: int, db: Session = Depends(get_db), user: User = Depends(current_user)):
    item = get_class_or_404(db, class_id); teacher_owns(user, item)
    ids = db.scalars(select(ClassStudent.student_id).where(ClassStudent.class_id == class_id)).all()
    students = db.scalars(select(User).where(User.id.in_(ids))).all() if ids else []
    return [user_dict(x) for x in students]


@app.post("/classes/{class_id}/notices")
def create_notice(class_id: int, payload: NoticeIn, db: Session = Depends(get_db), user: User = Depends(current_user)):
    item = get_class_or_404(db, class_id); teacher_owns(user, item)
    notice = Notice(class_id=class_id, author_id=user.id, title=payload.title.strip(), body=payload.body.strip())
    db.add(notice); db.commit(); db.refresh(notice)
    return {"id": notice.id, "title": notice.title, "body": notice.body, "created_at": notice.created_at}


@app.get("/classes/{class_id}/notices")
def list_notices(class_id: int, db: Session = Depends(get_db), user: User = Depends(current_user)):
    item = get_class_or_404(db, class_id); class_access(db, user, item)
    rows = db.scalars(select(Notice).where(Notice.class_id == class_id).order_by(Notice.created_at.desc())).all()
    return [{"id": x.id, "title": x.title, "body": x.body, "created_at": x.created_at, "updated_at": x.updated_at} for x in rows]


@app.put("/classes/{class_id}/attendance")
def set_attendance(class_id: int, payload: AttendanceIn, db: Session = Depends(get_db), user: User = Depends(current_user)):
    item = get_class_or_404(db, class_id); teacher_owns(user, item)
    member = db.scalar(select(ClassStudent).where(ClassStudent.class_id == class_id, ClassStudent.student_id == payload.student_id))
    if not member:
        raise HTTPException(400, "Student is not enrolled")
    row = db.scalar(select(Attendance).where(Attendance.class_id == class_id, Attendance.student_id == payload.student_id, Attendance.date == payload.date))
    if row:
        row.status, row.note = payload.status, payload.note
    else:
        row = Attendance(class_id=class_id, student_id=payload.student_id, date=payload.date, status=payload.status, note=payload.note); db.add(row)
    db.commit(); db.refresh(row)
    return {"id": row.id, "student_id": row.student_id, "date": row.date, "status": row.status, "note": row.note}


@app.get("/classes/{class_id}/attendance/me")
def my_attendance(class_id: int, db: Session = Depends(get_db), user: User = Depends(require_roles("student"))):
    student_member(db, user, class_id)
    rows = db.scalars(select(Attendance).where(Attendance.class_id == class_id, Attendance.student_id == user.id).order_by(Attendance.date.desc())).all()
    return [{"date": x.date, "status": x.status, "note": x.note} for x in rows]


@app.put("/classes/{class_id}/grades")
def set_grade(class_id: int, payload: GradeIn, db: Session = Depends(get_db), user: User = Depends(current_user)):
    item = get_class_or_404(db, class_id); teacher_owns(user, item)
    member = db.scalar(select(ClassStudent).where(ClassStudent.class_id == class_id, ClassStudent.student_id == payload.student_id))
    if not member:
        raise HTTPException(400, "Student is not enrolled")
    if payload.score > payload.max_score:
        raise HTTPException(400, "Score cannot exceed max_score")
    row = db.scalar(select(Grade).where(Grade.class_id == class_id, Grade.student_id == payload.student_id, Grade.category == payload.category, Grade.activity_key == payload.activity_key))
    if row:
        row.activity_name, row.score, row.max_score, row.source = payload.activity_name, payload.score, payload.max_score, payload.source
    else:
        row = Grade(class_id=class_id, **payload.model_dump()); db.add(row)
    db.commit(); db.refresh(row)
    return {"id": row.id, "student_id": row.student_id, "category": row.category, "activity_key": row.activity_key, "score": row.score, "max_score": row.max_score}


@app.get("/classes/{class_id}/grades/me")
def my_grades(class_id: int, db: Session = Depends(get_db), user: User = Depends(require_roles("student"))):
    student_member(db, user, class_id)
    rows = db.scalars(select(Grade).where(Grade.class_id == class_id, Grade.student_id == user.id).order_by(Grade.updated_at.desc())).all()
    return [{"category": x.category, "activity_key": x.activity_key, "activity_name": x.activity_name, "score": x.score, "max_score": x.max_score, "source": x.source} for x in rows]


@app.post("/classes/{class_id}/team-activities")
def create_team_activity(class_id: int, payload: TeamActivityIn, db: Session = Depends(get_db), user: User = Depends(require_roles("teacher"))):
    item = get_class_or_404(db, class_id); teacher_owns(user, item)
    enrolled = set(db.scalars(select(ClassStudent.student_id).where(ClassStudent.class_id == class_id)).all())
    requested = [sid for team in payload.teams for sid in team.student_ids]
    if len(requested) != len(set(requested)):
        raise HTTPException(400, "A student cannot appear in more than one team")
    if not set(requested).issubset(enrolled):
        raise HTTPException(400, "All team members must be enrolled")
    existing = db.scalar(select(TeamActivity).where(TeamActivity.class_id == class_id, TeamActivity.activity_key == payload.activity_key))
    teams_json = [team.model_dump() for team in payload.teams]
    if existing:
        if existing.closed:
            raise HTTPException(409, "Closed activity cannot be replaced")
        existing.name, existing.activity_type, existing.category, existing.teams = payload.name, payload.activity_type, payload.category, teams_json
        activity = existing
    else:
        activity = TeamActivity(class_id=class_id, teacher_id=user.id, name=payload.name, activity_type=payload.activity_type, category=payload.category, activity_key=payload.activity_key, teams=teams_json, base_scores={}, individual_scores={}); db.add(activity)
    db.commit(); db.refresh(activity)
    return {"id": activity.id, "class_id": class_id, "name": activity.name, "category": activity.category, "activity_key": activity.activity_key, "teams": activity.teams, "closed": activity.closed}


@app.get("/classes/{class_id}/team-activities")
def list_team_activities(class_id: int, db: Session = Depends(get_db), user: User = Depends(current_user)):
    if user.role == Role.DIRECTOR:
        raise HTTPException(403, "Directors cannot access student team details")
    item = get_class_or_404(db, class_id); class_access(db, user, item)
    rows = db.scalars(select(TeamActivity).where(TeamActivity.class_id == class_id).order_by(TeamActivity.created_at.desc())).all()
    result = []
    for x in rows:
        data = {"id": x.id, "name": x.name, "activity_type": x.activity_type, "category": x.category, "activity_key": x.activity_key, "closed": x.closed}
        if user.role == Role.STUDENT:
            data["team"] = next((t for t in (x.teams or []) if user.id in [int(i) for i in t.get("student_ids", [])]), None)
        else:
            data["teams"] = x.teams; data["base_scores"] = x.base_scores; data["individual_scores"] = x.individual_scores
        result.append(data)
    return result


@app.put("/team-activities/{activity_id}/scores")
def set_team_scores(activity_id: int, payload: TeamScoresIn, db: Session = Depends(get_db), user: User = Depends(require_roles("teacher"))):
    activity = db.get(TeamActivity, activity_id)
    if not activity:
        raise HTTPException(404, "Activity not found")
    item = get_class_or_404(db, activity.class_id); teacher_owns(user, item)
    allowed_teams = {t.get("name", "") for t in activity.teams or []}
    allowed_students = {str(i) for t in activity.teams or [] for i in t.get("student_ids", [])}
    if not set(payload.base_scores).issubset(allowed_teams):
        raise HTTPException(400, "Unknown team")
    if not set(payload.individual_scores).issubset(allowed_students):
        raise HTTPException(400, "Unknown team member")
    if any(v < 0 or v > 100 for v in list(payload.base_scores.values()) + list(payload.individual_scores.values())):
        raise HTTPException(400, "Scores must be 0..100")
    activity.base_scores = payload.base_scores; activity.individual_scores = payload.individual_scores
    if payload.close_and_consolidate:
        consolidate_team_scores(db, activity); activity.closed = True
    db.commit()
    return {"id": activity.id, "closed": activity.closed, "base_scores": activity.base_scores, "individual_scores": activity.individual_scores}


@app.post("/team-activities/{activity_id}/participation-reports")
def report_participation(activity_id: int, payload: ParticipationReportIn, db: Session = Depends(get_db), user: User = Depends(require_roles("student"))):
    activity = db.get(TeamActivity, activity_id)
    if not activity:
        raise HTTPException(404, "Activity not found")
    if activity.closed:
        raise HTTPException(409, "Activity is closed")
    student_member(db, user, activity.class_id)
    if payload.target_student_id == user.id:
        raise HTTPException(400, "You cannot report yourself")
    if not same_team(activity, user.id, payload.target_student_id):
        raise HTTPException(403, "You can only report a member of your own team")
    row = db.scalar(select(ParticipationReport).where(ParticipationReport.activity_id == activity_id, ParticipationReport.reporter_id == user.id, ParticipationReport.target_student_id == payload.target_student_id))
    if row:
        row.severity, row.comment = payload.severity, payload.comment.strip()
    else:
        row = ParticipationReport(activity_id=activity_id, reporter_id=user.id, target_student_id=payload.target_student_id, severity=payload.severity, comment=payload.comment.strip()); db.add(row)
    db.commit()
    return {"detail": "Anonymous participation report saved. Reporter identity is never exposed in teacher summaries."}


@app.get("/team-activities/{activity_id}/participation-summary")
def participation_summary(activity_id: int, db: Session = Depends(get_db), user: User = Depends(current_user)):
    activity = db.get(TeamActivity, activity_id)
    if not activity:
        raise HTTPException(404, "Activity not found")
    item = get_class_or_404(db, activity.class_id); teacher_owns(user, item)
    reports = db.scalars(select(ParticipationReport).where(ParticipationReport.activity_id == activity_id)).all()
    reviews = {r.student_id: r for r in db.scalars(select(ParticipationReview).where(ParticipationReview.activity_id == activity_id)).all()}
    by_target: dict[int, list[ParticipationReport]] = {}
    for report in reports:
        by_target.setdefault(report.target_student_id, []).append(report)
    out = []
    for student_id, rows in by_target.items():
        counts = Counter(r.severity for r in rows)
        review = reviews.get(student_id)
        out.append({"student_id": student_id, "report_count": len(rows), "partial": counts.get("partial", 0), "did_not_work": counts.get("did_not_work", 0), "requires_corroboration": len(rows) == 1, "resolution": review.resolution if review else None, "teacher_note": review.note if review else ""})
    return out


@app.put("/team-activities/{activity_id}/participation-review/{student_id}")
def review_participation(activity_id: int, student_id: int, payload: ParticipationReviewIn, db: Session = Depends(get_db), user: User = Depends(require_roles("teacher"))):
    activity = db.get(TeamActivity, activity_id)
    if not activity:
        raise HTTPException(404, "Activity not found")
    item = get_class_or_404(db, activity.class_id); teacher_owns(user, item)
    row = db.scalar(select(ParticipationReview).where(ParticipationReview.activity_id == activity_id, ParticipationReview.student_id == student_id))
    if row:
        row.resolution, row.note, row.teacher_id = payload.resolution, payload.note.strip(), user.id
    else:
        row = ParticipationReview(activity_id=activity_id, student_id=student_id, teacher_id=user.id, resolution=payload.resolution, note=payload.note.strip()); db.add(row)
    db.commit()
    return {"student_id": student_id, "resolution": payload.resolution, "note": payload.note}


@app.post("/schedule")
def create_schedule(payload: ScheduleIn, db: Session = Depends(get_db), user: User = Depends(require_roles("director"))):
    if not user.institution_id:
        raise HTTPException(400, "Director has no institution")
    teacher = db.get(User, payload.teacher_id)
    if not teacher or teacher.role != Role.TEACHER or teacher.institution_id != user.institution_id:
        raise HTTPException(400, "Teacher is not in your institution")
    if payload.class_id is not None:
        classroom = get_class_or_404(db, payload.class_id)
        if classroom.institution_id != user.institution_id:
            raise HTTPException(400, "Class is not in your institution")
        if classroom.teacher_id != payload.teacher_id:
            raise HTTPException(400, "Class belongs to another teacher")
    if payload.end_time <= payload.start_time:
        raise HTTPException(400, "end_time must be after start_time")
    conflicts = db.scalars(select(ScheduleEntry).where(ScheduleEntry.institution_id == user.institution_id, ScheduleEntry.weekday == payload.weekday, ScheduleEntry.teacher_id == payload.teacher_id, ScheduleEntry.start_time < payload.end_time, ScheduleEntry.end_time > payload.start_time)).all()
    if conflicts:
        raise HTTPException(409, "Teacher schedule conflict")
    if payload.room:
        room_conflicts = db.scalars(select(ScheduleEntry).where(ScheduleEntry.institution_id == user.institution_id, ScheduleEntry.weekday == payload.weekday, ScheduleEntry.room == payload.room, ScheduleEntry.start_time < payload.end_time, ScheduleEntry.end_time > payload.start_time)).all()
        if room_conflicts:
            raise HTTPException(409, "Room schedule conflict")
    row = ScheduleEntry(institution_id=user.institution_id, **payload.model_dump()); db.add(row); db.commit(); db.refresh(row)
    return {"id": row.id, **payload.model_dump()}


@app.get("/schedule")
def list_schedule(db: Session = Depends(get_db), user: User = Depends(current_user)):
    if user.role == Role.DIRECTOR:
        rows = db.scalars(select(ScheduleEntry).where(ScheduleEntry.institution_id == user.institution_id)).all() if user.institution_id else []
    elif user.role == Role.TEACHER:
        rows = db.scalars(select(ScheduleEntry).where(ScheduleEntry.teacher_id == user.id)).all()
    else:
        class_ids = db.scalars(select(ClassStudent.class_id).where(ClassStudent.student_id == user.id)).all()
        rows = db.scalars(select(ScheduleEntry).where(ScheduleEntry.class_id.in_(class_ids))).all() if class_ids else []
    return [{"id": x.id, "teacher_id": x.teacher_id, "class_id": x.class_id, "weekday": x.weekday, "start_time": x.start_time, "end_time": x.end_time, "room": x.room} for x in rows]
