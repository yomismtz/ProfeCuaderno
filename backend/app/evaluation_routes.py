from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field
from sqlalchemy import DateTime, Float, ForeignKey, Integer, String, UniqueConstraint, select
from sqlalchemy.orm import Mapped, Session, mapped_column

from .db import Base, get_db
from .main import get_class_or_404, student_member, teacher_owns
from .models import ClassStudent, Grade, Role, User
from .security import current_user


def utcnow():
    return datetime.now(timezone.utc)


class EvaluationPlanState(Base):
    __tablename__ = "evaluation_plan_state"

    class_id: Mapped[int] = mapped_column(ForeignKey("classes.id"), primary_key=True)
    finalized: Mapped[bool] = mapped_column(default=False)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)


class EvaluationPlanCategory(Base):
    __tablename__ = "evaluation_plan_categories"
    __table_args__ = (UniqueConstraint("class_id", "category_key"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    class_id: Mapped[int] = mapped_column(ForeignKey("classes.id"), index=True)
    category_key: Mapped[str] = mapped_column(String(160))
    name: Mapped[str] = mapped_column(String(160))
    weight: Mapped[float] = mapped_column(Float)
    mode: Mapped[str] = mapped_column(String(30), default="direct")
    position: Mapped[int] = mapped_column(Integer, default=0)
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=utcnow, onupdate=utcnow)


class EvaluationPlanCategoryIn(BaseModel):
    category_key: str = Field(min_length=1, max_length=160)
    name: str = Field(min_length=1, max_length=160)
    weight: float = Field(ge=0, le=100)
    mode: str = Field(pattern="^(direct|rubric|attendance|activities|exams)$")
    position: int = 0


class EvaluationPlanIn(BaseModel):
    finalized: bool = False
    categories: list[EvaluationPlanCategoryIn] = Field(default_factory=list)


router = APIRouter()


def category_dict(row: EvaluationPlanCategory) -> dict:
    return {
        "category_key": row.category_key,
        "name": row.name,
        "weight": row.weight,
        "mode": row.mode,
        "position": row.position,
    }


def plan_dict(db: Session, class_id: int) -> dict:
    state = db.get(EvaluationPlanState, class_id)
    categories = db.scalars(
        select(EvaluationPlanCategory)
        .where(EvaluationPlanCategory.class_id == class_id)
        .order_by(EvaluationPlanCategory.position, EvaluationPlanCategory.id)
    ).all()
    return {
        "class_id": class_id,
        "finalized": bool(state.finalized) if state else False,
        "categories": [category_dict(row) for row in categories],
    }


@router.put("/classes/{class_id}/evaluation-plan")
def set_evaluation_plan(
    class_id: int,
    payload: EvaluationPlanIn,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    classroom = get_class_or_404(db, class_id)
    teacher_owns(user, classroom)

    keys = [item.category_key.strip() for item in payload.categories]
    if len(keys) != len(set(keys)):
        raise HTTPException(400, "Evaluation category keys must be unique")
    if payload.finalized:
        if not payload.categories:
            raise HTTPException(400, "A finalized evaluation plan needs at least one category")
        total = sum(item.weight for item in payload.categories)
        if abs(total - 100.0) > 0.01:
            raise HTTPException(400, "Finalized evaluation category weights must sum to 100")

    existing = {
        row.category_key: row
        for row in db.scalars(
            select(EvaluationPlanCategory).where(EvaluationPlanCategory.class_id == class_id)
        ).all()
    }
    requested = set(keys)

    for item in payload.categories:
        key = item.category_key.strip()
        row = existing.get(key)
        if row is None:
            row = EvaluationPlanCategory(class_id=class_id, category_key=key)
            db.add(row)
        row.name = item.name.strip()
        row.weight = float(item.weight)
        row.mode = item.mode
        row.position = item.position

    for key, row in existing.items():
        if key not in requested:
            db.delete(row)

    # Las calificaciones-resumen del docente usan la misma category_key como
    # activity_key. Si un rubro fue eliminado localmente, se elimina también su
    # valor remoto para que el alumno no siga viendo una calificación obsoleta.
    category_grades = db.scalars(
        select(Grade).where(
            Grade.class_id == class_id,
            Grade.activity_key.like("category-%"),
        )
    ).all()
    for grade in category_grades:
        if grade.activity_key not in requested:
            db.delete(grade)

    state = db.get(EvaluationPlanState, class_id)
    if state is None:
        state = EvaluationPlanState(class_id=class_id)
        db.add(state)
    state.finalized = payload.finalized
    db.commit()
    return plan_dict(db, class_id)


@router.get("/classes/{class_id}/evaluation-plan")
def get_evaluation_plan(
    class_id: int,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    classroom = get_class_or_404(db, class_id)
    if user.role == Role.DIRECTOR:
        raise HTTPException(403, "Directors cannot access student evaluation details")
    if user.role == Role.TEACHER:
        teacher_owns(user, classroom)
    elif user.role == Role.STUDENT:
        student_member(db, user, class_id)
    else:
        raise HTTPException(403, "Not allowed for this class")
    return plan_dict(db, class_id)


@router.delete("/classes/{class_id}/grades/{student_id}/{activity_key}")
def delete_grade(
    class_id: int,
    student_id: int,
    activity_key: str,
    db: Session = Depends(get_db),
    user: User = Depends(current_user),
):
    classroom = get_class_or_404(db, class_id)
    teacher_owns(user, classroom)
    member = db.scalar(
        select(ClassStudent).where(
            ClassStudent.class_id == class_id,
            ClassStudent.student_id == student_id,
        )
    )
    if not member:
        raise HTTPException(400, "Student is not enrolled")

    rows = db.scalars(
        select(Grade).where(
            Grade.class_id == class_id,
            Grade.student_id == student_id,
            Grade.activity_key == activity_key,
        )
    ).all()
    for row in rows:
        db.delete(row)
    db.commit()
    return {"detail": "Grade removed", "deleted": len(rows)}
