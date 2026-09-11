import os
from uuid import uuid4

os.environ["DATABASE_URL"] = "sqlite:///./test_backend.db"
os.environ["JWT_SECRET"] = "test-secret"

from fastapi.testclient import TestClient
from app.web import app


def register(client: TestClient, role: str):
    email = f"evaluation-{role}-{uuid4().hex}@example.com"
    response = client.post(
        "/auth/register",
        json={"email": email, "password": "password123", "full_name": role.title(), "role": role},
    )
    assert response.status_code == 200, response.text
    data = response.json()
    return email, data["user"], {"Authorization": f"Bearer {data['access_token']}"}


def test_evaluation_plan_student_visibility_director_privacy_and_grade_delete():
    with TestClient(app) as client:
        _, director, director_headers = register(client, "director")
        teacher_email, _, teacher_headers = register(client, "teacher")
        _, student, student_headers = register(client, "student")

        institution = client.post(
            "/institutions",
            json={"name": f"Evaluation School {uuid4().hex[:8]}"},
            headers=director_headers,
        )
        assert institution.status_code == 200, institution.text
        attach = client.post(
            "/institutions/attach-teacher",
            params={"email": teacher_email},
            headers=director_headers,
        )
        assert attach.status_code == 200, attach.text

        classroom_response = client.post(
            "/classes",
            json={"name": "2A", "subject": "Matemáticas", "period_name": "Parcial 1"},
            headers=teacher_headers,
        )
        assert classroom_response.status_code == 200, classroom_response.text
        classroom = classroom_response.json()

        join = client.post(
            "/classes/join",
            json={"class_code": classroom["class_code"]},
            headers=student_headers,
        )
        assert join.status_code == 200, join.text

        plan = {
            "finalized": True,
            "categories": [
                {"category_key": "category-1", "name": "Exámenes", "weight": 60.0, "mode": "exams", "position": 0},
                {"category_key": "category-2", "name": "Asistencia", "weight": 40.0, "mode": "attendance", "position": 1},
            ],
        }
        saved = client.put(
            f"/classes/{classroom['id']}/evaluation-plan",
            json=plan,
            headers=teacher_headers,
        )
        assert saved.status_code == 200, saved.text
        assert saved.json()["finalized"] is True
        assert sum(item["weight"] for item in saved.json()["categories"]) == 100.0

        student_plan = client.get(
            f"/classes/{classroom['id']}/evaluation-plan",
            headers=student_headers,
        )
        assert student_plan.status_code == 200, student_plan.text
        assert [item["category_key"] for item in student_plan.json()["categories"]] == ["category-1", "category-2"]

        director_plan = client.get(
            f"/classes/{classroom['id']}/evaluation-plan",
            headers=director_headers,
        )
        assert director_plan.status_code == 403

        bad_plan = client.put(
            f"/classes/{classroom['id']}/evaluation-plan",
            json={
                "finalized": True,
                "categories": [
                    {"category_key": "bad", "name": "Incompleto", "weight": 90.0, "mode": "direct", "position": 0}
                ],
            },
            headers=teacher_headers,
        )
        assert bad_plan.status_code == 400

        zero_grade = client.put(
            f"/classes/{classroom['id']}/grades",
            json={
                "student_id": student["id"],
                "category": "Exámenes",
                "activity_key": "category-1",
                "activity_name": "Exámenes",
                "score": 0.0,
                "max_score": 100.0,
                "source": "teacher_app",
            },
            headers=teacher_headers,
        )
        assert zero_grade.status_code == 200, zero_grade.text

        grades = client.get(f"/classes/{classroom['id']}/grades/me", headers=student_headers)
        assert grades.status_code == 200, grades.text
        assert grades.json()[0]["score"] == 0.0

        deleted = client.delete(
            f"/classes/{classroom['id']}/grades/{student['id']}/category-1",
            headers=teacher_headers,
        )
        assert deleted.status_code == 200, deleted.text
        assert deleted.json()["deleted"] == 1

        grades_after = client.get(f"/classes/{classroom['id']}/grades/me", headers=student_headers)
        assert grades_after.status_code == 200
        assert grades_after.json() == []
