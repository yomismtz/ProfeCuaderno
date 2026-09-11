import os
from uuid import uuid4

os.environ["DATABASE_URL"] = "sqlite:///./test_backend.db"
os.environ["JWT_SECRET"] = "test-secret"

from fastapi.testclient import TestClient
from app.web import app


def register(client: TestClient, role: str):
    email = f"attendance-{role}-{uuid4().hex}@example.com"
    response = client.post(
        "/auth/register",
        json={"email": email, "password": "password123", "full_name": role.title(), "role": role},
    )
    assert response.status_code == 200, response.text
    data = response.json()
    return email, data["user"], {"Authorization": f"Bearer {data['access_token']}"}


def test_attendance_policy_sessions_and_privacy_summary():
    with TestClient(app) as client:
        _, director, director_headers = register(client, "director")
        teacher_email, _, teacher_headers = register(client, "teacher")
        _, student, student_headers = register(client, "student")

        institution = client.post(
            "/institutions",
            json={"name": f"Attendance School {uuid4().hex[:8]}"},
            headers=director_headers,
        )
        assert institution.status_code == 200, institution.text

        classroom = client.post(
            "/classes",
            json={"name": "1A", "subject": "Prueba", "period_name": "2026"},
            headers=teacher_headers,
        )
        assert classroom.status_code == 200, classroom.text
        class_id = classroom.json()["id"]
        class_code = classroom.json()["class_code"]

        attached = client.post(
            "/institutions/attach-teacher",
            params={"email": teacher_email},
            headers=director_headers,
        )
        assert attached.status_code == 200, attached.text

        joined = client.post(
            "/classes/join",
            json={"class_code": class_code},
            headers=student_headers,
        )
        assert joined.status_code == 200, joined.text

        policy = client.put(
            f"/classes/{class_id}/attendance-policy",
            json={"late_per_absence": 3, "justified_effect": "absent"},
            headers=teacher_headers,
        )
        assert policy.status_code == 200, policy.text
        assert policy.json()["late_per_absence"] == 3
        assert policy.json()["justified_effect"] == "absent"

        dates = ["2026-09-01", "2026-09-02", "2026-09-03", "2026-09-04"]
        titles = ["Introducción", "Práctica", "Seminario", "Laboratorio"]
        statuses = ["late", "late", "late", "justified"]
        for date, title, status in zip(dates, titles, statuses):
            session = client.put(
                f"/classes/{class_id}/attendance-session",
                json={"date": date, "title": title, "worked": True},
                headers=teacher_headers,
            )
            assert session.status_code == 200, session.text
            attendance = client.put(
                f"/classes/{class_id}/attendance",
                json={"student_id": student["id"], "date": date, "status": status, "note": ""},
                headers=teacher_headers,
            )
            assert attendance.status_code == 200, attendance.text

        summary = client.get(
            f"/classes/{class_id}/attendance-summary/me",
            headers=student_headers,
        )
        assert summary.status_code == 200, summary.text
        body = summary.json()
        assert body["late_penalties"] == 1
        assert body["effective_absences"] == 2
        assert body["attendance_percent"] == 50.0

        sessions = client.get(f"/classes/{class_id}/attendance-sessions", headers=student_headers)
        assert sessions.status_code == 200, sessions.text
        assert {row["title"] for row in sessions.json()} >= set(titles)

        records = client.get(
            f"/classes/{class_id}/attendance-records/{student['id']}",
            headers=teacher_headers,
        )
        assert records.status_code == 200, records.text
        assert len(records.json()["records"]) == 4
        assert {row["title"] for row in records.json()["records"]} == set(titles)

        institutional = client.get("/institutions/attendance-summary", headers=director_headers)
        assert institutional.status_code == 200, institutional.text
        row = next(item for item in institutional.json() if item["class_id"] == class_id)
        assert row["attendance_percent"] == 50.0
        assert row["effective_absences"] == 2
        assert "student_id" not in row
