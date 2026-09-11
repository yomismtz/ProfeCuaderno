from typing import Any
from pydantic import BaseModel, EmailStr, Field


class RegisterIn(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    full_name: str = Field(default="", max_length=200)
    role: str = Field(pattern="^(teacher|student|director)$")


class LoginIn(BaseModel):
    email: EmailStr
    password: str


class InstitutionIn(BaseModel):
    name: str = Field(min_length=2, max_length=200)


class ClassIn(BaseModel):
    name: str = Field(min_length=1, max_length=160)
    subject: str = Field(default="", max_length=160)
    period_name: str = Field(default="", max_length=160)


class JoinClassIn(BaseModel):
    class_code: str = Field(min_length=4, max_length=12)


class NoticeIn(BaseModel):
    title: str = Field(min_length=1, max_length=200)
    body: str = Field(min_length=1, max_length=10000)


class AttendanceIn(BaseModel):
    student_id: int
    date: str = Field(pattern=r"^\d{4}-\d{2}-\d{2}$")
    status: str = Field(pattern="^(present|absent|late|justified)$")
    note: str = Field(default="", max_length=500)


class GradeIn(BaseModel):
    student_id: int
    category: str = Field(min_length=1, max_length=120)
    activity_key: str = Field(min_length=1, max_length=160)
    activity_name: str = Field(min_length=1, max_length=200)
    score: float = Field(ge=0)
    max_score: float = Field(default=100.0, gt=0)
    source: str = Field(default="manual", max_length=40)


class TeamIn(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    student_ids: list[int] = Field(min_length=1)


class TeamActivityIn(BaseModel):
    name: str = Field(min_length=1, max_length=200)
    activity_type: str = Field(default="Exposición", max_length=80)
    category: str = Field(min_length=1, max_length=120)
    activity_key: str = Field(min_length=1, max_length=160)
    teams: list[TeamIn] = Field(min_length=1)


class TeamScoresIn(BaseModel):
    base_scores: dict[str, float] = Field(default_factory=dict)
    individual_scores: dict[str, float] = Field(default_factory=dict)
    close_and_consolidate: bool = False


class ParticipationReportIn(BaseModel):
    target_student_id: int
    severity: str = Field(pattern="^(partial|did_not_work)$")
    comment: str = Field(default="", max_length=500)


class ParticipationReviewIn(BaseModel):
    resolution: str = Field(pattern="^(confirmed|not_confirmed|partial|insufficient_evidence)$")
    note: str = Field(default="", max_length=500)


class ScheduleIn(BaseModel):
    teacher_id: int
    class_id: int | None = None
    weekday: int = Field(ge=1, le=7)
    start_time: str = Field(pattern=r"^\d{2}:\d{2}$")
    end_time: str = Field(pattern=r"^\d{2}:\d{2}$")
    room: str = Field(default="", max_length=120)


class ApiMessage(BaseModel):
    detail: str


class TokenOut(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: dict[str, Any]
