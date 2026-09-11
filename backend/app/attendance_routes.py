from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from .db import get_db
from .models import (
    Attendance,
    AttendancePolicy,
    AttendanceSessionMeta,
    ClassStudent,
    Role,
    SchoolClass,
    User,
)
from .security import current_user

router = APIRouter(tags=["attendance-v2"])


class AttendancePolicyPayload(BaseModel):
    late_per_absence: int = Field(default=3, ge=2, le=10)
    justified_effect: str = Field(default="present", pattern="^(present|late|absent|excluded)$")


class AttendanceSessionPayload(BaseModel):
    date: str = Field(pattern=r"^\d{4}-\d{2}-\d{2}$")
    title: str = Field(default="Clase", max_length=160)
    worked: bool = True


def _class(db: Session, class_id: int) -> SchoolClass:
    item = db.get(SchoolClass, class_id)
    if not item:
        raise HTTPException(404, "Class not found")
    return item


def _teacher_owns(user: User, item: SchoolClass):
    if user.role != Role.TEACHER or item.teacher_id != user.id:
        raise HTTPException(403, "Not allowed for this class")


def _member(db: Session, user: User, class_id: int):
    found = db.scalar(
        select(ClassStudent).where(
            ClassStudent.class_id == class_id,
            ClassStudent.student_id == user.id,
        )
    )
    if not found:
        raise HTTPException(403, "Student is not enrolled in this class")


def _class_access(db: Session, user: User, item: SchoolClass):
    if user.role == Role.TEACHER and item.teacher_id == user.id:
        return
    if user.role == Role.STUDENT:
        _member(db, user, item.id)
        return
    if user.role == Role.DIRECTOR and user.institution_id and user.institution_id == item.institution_id:
        return
    raise HTTPException(403, "Not allowed for this class")


def _policy(db: Session, class_id: int) -> AttendancePolicy:
    row = db.get(AttendancePolicy, class_id)
    if row is None:
        row = AttendancePolicy(class_id=class_id, late_per_absence=3, justified_effect="present")
        db.add(row)
        db.commit()
        db.refresh(row)
    return row


def _policy_dict(row: AttendancePolicy) -> dict:
    return {
        "class_id": row.class_id,
        "late_per_absence": max(2, min(10, row.late_per_absence)),
        "justified_effect": row.justified_effect,
        "updated_at": row.updated_at,
    }


def _metrics(rows: list[Attendance], policy: AttendancePolicy) -> dict:
    counts = {"present": 0, "absent": 0, "late": 0, "justified": 0}
    for row in rows:
        status = (row.status or "").strip().lower()
        if status in counts:
            counts[status] += 1

    present = counts["present"]
    absent = counts["absent"]
    late = counts["late"]
    justified = counts["justified"]

    effect = policy.justified_effect
    if effect == "present":
        present += justified
    elif effect == "late":
        late += justified
    elif effect == "absent":
        absent += justified

    late_penalties = late // max(2, min(10, policy.late_per_absence))
    earned = present + (late - late_penalties)
    denominator = present + absent + late
    if effect == "excluded":
        denominator = counts["present"] + counts["absent"] + counts["late"]

    return {
        "counts": counts,
        "late_penalties": late_penalties,
        "effective_absences": absent + late_penalties,
        "earned": earned,
        "denominator": denominator,
    }


def _summary(rows: list[Attendance], policy: AttendancePolicy) -> dict:
    metrics = _metrics(rows, policy)
    denominator = metrics["denominator"]
    percentage = 0.0 if denominator == 0 else (metrics["earned"] / denominator) * 100.0
    return {
        "records": len(rows),
        "counts": metrics["counts"],
        "late_penalties": metrics["late_penalties"],
        "effective_absences": metrics["effective_absences"],
        "attendance_percent": round(percentage, 2),
        "policy": _policy_dict(policy),
    }


def _aggregate_summary(rows: list[Attendance], policy: AttendancePolicy) -> dict:
    """Apply attendance penalties per student first, then aggregate the class."""
    groups: dict[int, list[Attendance]] = {}
    for row in rows:
        groups.setdefault(row.student_id, []).append(row)

    counts = {"present": 0, "absent": 0, "late": 0, "justified": 0}
    total_penalties = 0
    total_effective_absences = 0
    total_earned = 0
    total_denominator = 0

    for student_rows in groups.values():
        metrics = _metrics(student_rows, policy)
        for key in counts:
            counts[key] += metrics["counts"][key]
        total_penalties += metrics["late_penalties"]
        total_effective_absences += metrics["effective_absences"]
        total_earned += metrics["earned"]
        total_denominator += metrics["denominator"]

    percentage = 0.0 if total_denominator == 0 else (total_earned / total_denominator) * 100.0
    return {
        "records": len(rows),
        "counts": counts,
        "late_penalties": total_penalties,
        "effective_absences": total_effective_absences,
        "attendance_percent": round(percentage, 2),
        "policy": _policy_dict(policy),
    }


def _session_map(db: Session, class_id: int) -> dict[str, AttendanceSessionMeta]:
    rows = db.scalars(
        select(AttendanceSessionMeta)
        .where(AttendanceSessionMeta.class_id == class_id)
        .order_by(AttendanceSessionMeta.date.desc())
    ).all()
    return {row.date: row for row in rows}


def _countable_rows(db: Session, class_id: int, rows: list[Attendance]) -> list[Attendance]:
    """Keep legacy rows countable unless an explicit session metadata row marks the date suspended."""
    metadata = _session_map(db, class_id)
    return [row for row in rows if row.date not in metadata or metadata[row.date].worked]


@router.get("/classes/{class_id}/attendance-policy")
def get_attendance_policy(
    class_id: int,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    item = _class(db, class_id)
    _class_access(db, user, item)
    return _policy_dict(_policy(db, class_id))


@router.put("/classes/{class_id}/attendance-policy")
def set_attendance_policy(
    class_id: int,
    payload: AttendancePolicyPayload,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    item = _class(db, class_id)
    _teacher_owns(user, item)
    row = _policy(db, class_id)
    row.late_per_absence = payload.late_per_absence
    row.justified_effect = payload.justified_effect
    db.commit()
    db.refresh(row)
    return _policy_dict(row)


@router.put("/classes/{class_id}/attendance-session")
def set_attendance_session(
    class_id: int,
    payload: AttendanceSessionPayload,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    item = _class(db, class_id)
    _teacher_owns(user, item)
    row = db.scalar(
        select(AttendanceSessionMeta).where(
            AttendanceSessionMeta.class_id == class_id,
            AttendanceSessionMeta.date == payload.date,
        )
    )
    if row is None:
        row = AttendanceSessionMeta(class_id=class_id, date=payload.date)
        db.add(row)
    row.title = payload.title.strip() or "Clase"
    row.worked = payload.worked
    db.commit()
    db.refresh(row)
    return {
        "date": row.date,
        "title": row.title,
        "worked": row.worked,
        "updated_at": row.updated_at,
    }


@router.get("/classes/{class_id}/attendance-sessions")
def attendance_sessions(
    class_id: int,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    item = _class(db, class_id)
    _class_access(db, user, item)
    metadata = _session_map(db, class_id)
    dates = db.scalars(
        select(Attendance.date)
        .where(Attendance.class_id == class_id)
        .distinct()
        .order_by(Attendance.date.desc())
    ).all()
    all_dates = sorted(set(dates) | set(metadata.keys()), reverse=True)
    return [
        {
            "date": date,
            "title": metadata[date].title if date in metadata else "Clase",
            "worked": metadata[date].worked if date in metadata else True,
        }
        for date in all_dates
    ]


@router.get("/classes/{class_id}/attendance-summary/me")
def my_attendance_summary(
    class_id: int,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    item = _class(db, class_id)
    if user.role != Role.STUDENT:
        raise HTTPException(403, "Student account required")
    _member(db, user, class_id)
    rows = db.scalars(
        select(Attendance)
        .where(Attendance.class_id == class_id, Attendance.student_id == user.id)
        .order_by(Attendance.date.desc())
    ).all()
    result = _summary(_countable_rows(db, class_id, rows), _policy(db, class_id))
    result["student_id"] = user.id
    return result


@router.get("/classes/{class_id}/attendance-records/{student_id}")
def student_attendance_records(
    class_id: int,
    student_id: int,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    item = _class(db, class_id)
    _teacher_owns(user, item)
    membership = db.scalar(
        select(ClassStudent).where(
            ClassStudent.class_id == class_id,
            ClassStudent.student_id == student_id,
        )
    )
    if not membership:
        raise HTTPException(404, "Student is not enrolled in this class")
    rows = db.scalars(
        select(Attendance)
        .where(Attendance.class_id == class_id, Attendance.student_id == student_id)
        .order_by(Attendance.date.desc())
    ).all()
    metadata = _session_map(db, class_id)
    policy = _policy(db, class_id)
    return {
        "student_id": student_id,
        "summary": _summary(_countable_rows(db, class_id, rows), policy),
        "records": [
            {
                "id": row.id,
                "date": row.date,
                "title": metadata[row.date].title if row.date in metadata else "Clase",
                "worked": metadata[row.date].worked if row.date in metadata else True,
                "status": row.status,
                "note": row.note,
            }
            for row in rows
        ],
    }


@router.get("/classes/{class_id}/attendance-summary")
def class_attendance_summary(
    class_id: int,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    item = _class(db, class_id)
    _class_access(db, user, item)
    if user.role == Role.STUDENT:
        raise HTTPException(403, "Not available for student accounts")
    policy = _policy(db, class_id)
    student_ids = db.scalars(
        select(ClassStudent.student_id).where(ClassStudent.class_id == class_id)
    ).all()
    rows = db.scalars(select(Attendance).where(Attendance.class_id == class_id)).all()
    overall = _aggregate_summary(_countable_rows(db, class_id, rows), policy)
    overall.update({
        "class_id": class_id,
        "class_name": item.name,
        "teacher_id": item.teacher_id,
        "enrolled_students": len(student_ids),
    })
    return overall


@router.get("/institutions/attendance-summary")
def institution_attendance_summary(
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    if user.role != Role.DIRECTOR:
        raise HTTPException(403, "Director account required")
    if not user.institution_id:
        return []
    classes = db.scalars(
        select(SchoolClass)
        .where(SchoolClass.institution_id == user.institution_id, SchoolClass.active.is_(True))
        .order_by(SchoolClass.name)
    ).all()
    output = []
    for item in classes:
        policy = _policy(db, item.id)
        rows = db.scalars(select(Attendance).where(Attendance.class_id == item.id)).all()
        enrolled = db.scalar(
            select(func.count(ClassStudent.id)).where(ClassStudent.class_id == item.id)
        ) or 0
        summary = _aggregate_summary(_countable_rows(db, item.id, rows), policy)
        output.append({
            "class_id": item.id,
            "class_name": item.name,
            "subject": item.subject,
            "teacher_id": item.teacher_id,
            "enrolled_students": enrolled,
            "attendance_percent": summary["attendance_percent"],
            "records": summary["records"],
            "counts": summary["counts"],
            "effective_absences": summary["effective_absences"],
        })
    return output
