import os
from uuid import uuid4

os.environ["DATABASE_URL"] = "sqlite:///./test_backend.db"
os.environ["JWT_SECRET"] = "test-secret"

from fastapi.testclient import TestClient
from app.main import app


def unique_email(prefix: str) -> str:
    return f"{prefix}-{uuid4().hex}@example.com"


def register(client: TestClient, role: str, name: str):
    email = unique_email(role)
    response = client.post(
        "/auth/register",
        json={"email": email, "password": "password123", "full_name": name, "role": role},
    )
    assert response.status_code == 200, response.text
    data = response.json()
    headers = {"Authorization": f"Bearer {data['access_token']}"}
    return email, data["user"], headers


def test_health():
    with TestClient(app) as client:
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "ok"


def test_register_and_login_student():
    with TestClient(app) as client:
        email = unique_email("student-login")
        payload = {"email": email, "password": "password123", "full_name": "Student Test", "role": "student"}
        response = client.post("/auth/register", json=payload)
        assert response.status_code == 200
        response = client.post("/auth/login", json={"email": email, "password": "password123"})
        assert response.status_code == 200
        assert response.json()["user"]["role"] == "student"


def test_complete_teacher_student_director_flow_and_privacy():
    with TestClient(app) as client:
        _, director, director_headers = register(client, "director", "Director Test")
        teacher_email, teacher, teacher_headers = register(client, "teacher", "Teacher Test")
        teacher2_email, teacher2, teacher2_headers = register(client, "teacher", "Teacher Two")
        _, student, student_headers = register(client, "student", "Student Test")
        _, teammate, teammate_headers = register(client, "student", "Team Mate")
        _, other_team_student, other_team_headers = register(client, "student", "Other Team")
        _, outsider, _ = register(client, "student", "Outsider Student")

        institution_name = f"School {uuid4().hex[:10]}"
        institution = client.post(
            "/institutions",
            json={"name": institution_name},
            headers=director_headers,
        )
        assert institution.status_code == 200, institution.text
        institution_id = institution.json()["id"]

        _, _, duplicate_director_headers = register(client, "director", "Duplicate Director")
        duplicate_school = client.post(
            "/institutions",
            json={"name": institution_name.lower()},
            headers=duplicate_director_headers,
        )
        assert duplicate_school.status_code == 409

        classroom = client.post(
            "/classes",
            json={"name": "1A", "subject": "Matemáticas", "period_name": "2026-2027"},
            headers=teacher_headers,
        )
        assert classroom.status_code == 200, classroom.text
        classroom_data = classroom.json()
        class_id = classroom_data["id"]
        assert classroom_data["institution_id"] is None

        classroom2 = client.post(
            "/classes",
            json={"name": "2B", "subject": "Ciencias", "period_name": "2026-2027"},
            headers=teacher2_headers,
        )
        assert classroom2.status_code == 200, classroom2.text
        classroom2_data = classroom2.json()
        class2_id = classroom2_data["id"]

        attached = client.post(
            "/institutions/attach-teacher",
            params={"email": teacher_email},
            headers=director_headers,
        )
        assert attached.status_code == 200, attached.text
        assert attached.json()["institution_id"] == institution_id

        attached2 = client.post(
            "/institutions/attach-teacher",
            params={"email": teacher2_email},
            headers=director_headers,
        )
        assert attached2.status_code == 200, attached2.text

        director_classes = client.get("/classes", headers=director_headers)
        assert director_classes.status_code == 200
        assert any(row["id"] == class_id and row["institution_id"] == institution_id for row in director_classes.json())
        assert any(row["id"] == class2_id and row["institution_id"] == institution_id for row in director_classes.json())

        teachers = client.get("/institutions/teachers", headers=director_headers)
        assert teachers.status_code == 200
        assert any(row["id"] == teacher["id"] for row in teachers.json())
        assert any(row["id"] == teacher2["id"] for row in teachers.json())

        institution_get = client.get("/institution", headers=teacher_headers)
        assert institution_get.status_code == 200
        assert institution_get.json() == {"id": institution_id, "name": institution_name}

        assert client.get(f"/classes/{class_id}/students", headers=director_headers).status_code == 403

        for headers in (student_headers, teammate_headers, other_team_headers):
            joined = client.post(
                "/classes/join",
                json={"class_code": classroom_data["class_code"]},
                headers=headers,
            )
            assert joined.status_code == 200, joined.text

        student_me = client.get("/me", headers=student_headers)
        assert student_me.status_code == 200
        assert student_me.json()["institution_id"] == institution_id
        assert client.get("/institution", headers=student_headers).json()["name"] == institution_name

        roster = client.get(f"/classes/{class_id}/students", headers=teacher_headers)
        assert roster.status_code == 200
        assert {row["id"] for row in roster.json()} == {student["id"], teammate["id"], other_team_student["id"]}

        valid_grade = {
            "student_id": student["id"],
            "category": "Examen",
            "activity_key": "exam-1",
            "activity_name": "Examen 1",
            "score": 85,
            "max_score": 100,
        }
        grade = client.put(f"/classes/{class_id}/grades", json=valid_grade, headers=teacher_headers)
        assert grade.status_code == 200, grade.text

        not_enrolled_grade = dict(valid_grade, student_id=outsider["id"], activity_key="outsider")
        assert client.put(f"/classes/{class_id}/grades", json=not_enrolled_grade, headers=teacher_headers).status_code == 400

        excessive_grade = dict(valid_grade, score=101, activity_key="too-high")
        assert client.put(f"/classes/{class_id}/grades", json=excessive_grade, headers=teacher_headers).status_code == 400

        assert client.put(f"/classes/{class_id}/grades", json=valid_grade, headers=director_headers).status_code == 403
        assert client.put(
            f"/classes/{class_id}/attendance",
            json={"student_id": student["id"], "date": "2026-09-10", "status": "present", "note": ""},
            headers=director_headers,
        ).status_code == 403
        assert client.post(
            f"/classes/{class_id}/notices",
            json={"title": "No debe llegar", "body": "Aviso de dirección no debe ser aviso de clase"},
            headers=director_headers,
        ).status_code == 403

        class_notice = client.post(
            f"/classes/{class_id}/notices",
            json={"title": "Tarea", "body": "Revisar capítulo 2"},
            headers=teacher_headers,
        )
        assert class_notice.status_code == 200
        student_notices = client.get(f"/classes/{class_id}/notices", headers=student_headers)
        assert student_notices.status_code == 200
        assert student_notices.json()[0]["title"] == "Tarea"

        director_notice = client.post(
            "/institutions/notices",
            json={"title": "Consejo técnico", "body": "Reunión el viernes"},
            headers=director_headers,
        )
        assert director_notice.status_code == 200, director_notice.text
        teacher_notices = client.get("/teacher-notices", headers=teacher_headers)
        assert teacher_notices.status_code == 200
        assert teacher_notices.json()[0]["title"] == "Consejo técnico"
        assert client.get("/teacher-notices", headers=student_headers).status_code == 403

        first_slot = client.post(
            "/schedule",
            json={
                "teacher_id": teacher["id"],
                "class_id": class_id,
                "weekday": 1,
                "start_time": "07:00",
                "end_time": "07:50",
                "room": "A1",
            },
            headers=director_headers,
        )
        assert first_slot.status_code == 200, first_slot.text
        first_slot_id = first_slot.json()["id"]

        teacher_conflict = client.post(
            "/schedule",
            json={
                "teacher_id": teacher["id"],
                "class_id": class_id,
                "weekday": 1,
                "start_time": "07:30",
                "end_time": "08:10",
                "room": "A2",
            },
            headers=director_headers,
        )
        assert teacher_conflict.status_code == 409

        room_conflict = client.post(
            "/schedule",
            json={
                "teacher_id": teacher2["id"],
                "class_id": class2_id,
                "weekday": 1,
                "start_time": "07:20",
                "end_time": "08:00",
                "room": "A1",
            },
            headers=director_headers,
        )
        assert room_conflict.status_code == 409

        second_slot = client.post(
            "/schedule",
            json={
                "teacher_id": teacher2["id"],
                "class_id": class2_id,
                "weekday": 1,
                "start_time": "07:20",
                "end_time": "08:00",
                "room": "B2",
            },
            headers=director_headers,
        )
        assert second_slot.status_code == 200, second_slot.text

        student_schedule = client.get("/schedule", headers=student_headers)
        assert student_schedule.status_code == 200
        assert [row["id"] for row in student_schedule.json()] == [first_slot_id]

        deleted = client.delete(f"/schedule/{first_slot_id}", headers=director_headers)
        assert deleted.status_code == 200
        assert client.get("/schedule", headers=student_headers).json() == []

        # Real team flow: students only see their own team and anonymous reports never expose the reporter.
        team_activity = client.post(
            f"/classes/{class_id}/team-activities",
            json={
                "name": "Proyecto",
                "activity_type": "Proyecto",
                "category": "Proyecto",
                "activity_key": "project-1",
                "teams": [
                    {"name": "Equipo 1", "student_ids": [student["id"], teammate["id"]]},
                    {"name": "Equipo 2", "student_ids": [other_team_student["id"]]},
                ],
            },
            headers=teacher_headers,
        )
        assert team_activity.status_code == 200, team_activity.text
        activity_id = team_activity.json()["id"]
        assert client.get(f"/classes/{class_id}/team-activities", headers=director_headers).status_code == 403

        student_activities = client.get(f"/classes/{class_id}/team-activities", headers=student_headers)
        assert student_activities.status_code == 200
        activity = student_activities.json()[0]
        assert activity["team"]["name"] == "Equipo 1"
        assert {member["id"] for member in activity["team_members"]} == {student["id"], teammate["id"]}
        assert {member["full_name"] for member in activity["team_members"]} == {"Student Test", "Team Mate"}
        assert all("email" not in member for member in activity["team_members"])
        assert other_team_student["id"] not in {member["id"] for member in activity["team_members"]}

        own_team_report = client.post(
            f"/team-activities/{activity_id}/participation-reports",
            json={"target_student_id": teammate["id"], "severity": "partial", "comment": "Participó poco"},
            headers=student_headers,
        )
        assert own_team_report.status_code == 200, own_team_report.text
        assert client.post(
            f"/team-activities/{activity_id}/participation-reports",
            json={"target_student_id": student["id"], "severity": "partial", "comment": ""},
            headers=student_headers,
        ).status_code == 400
        assert client.post(
            f"/team-activities/{activity_id}/participation-reports",
            json={"target_student_id": other_team_student["id"], "severity": "did_not_work", "comment": ""},
            headers=student_headers,
        ).status_code == 403

        summary = client.get(f"/team-activities/{activity_id}/participation-summary", headers=teacher_headers)
        assert summary.status_code == 200
        rows = summary.json()
        assert rows == [{
            "student_id": teammate["id"],
            "report_count": 1,
            "partial": 1,
            "did_not_work": 0,
            "requires_corroboration": True,
            "resolution": None,
            "teacher_note": "",
        }]
        assert "reporter_id" not in str(rows)

        reviewed = client.put(
            f"/team-activities/{activity_id}/participation-review/{teammate['id']}",
            json={"resolution": "confirmed", "note": "Corroborado por el docente"},
            headers=teacher_headers,
        )
        assert reviewed.status_code == 200
        reviewed_summary = client.get(f"/team-activities/{activity_id}/participation-summary", headers=teacher_headers).json()
        assert reviewed_summary[0]["resolution"] == "confirmed"

        closed = client.put(
            f"/team-activities/{activity_id}/scores",
            json={"base_scores": {}, "individual_scores": {}, "close_and_consolidate": True},
            headers=teacher_headers,
        )
        assert closed.status_code == 200
        assert closed.json()["closed"] is True
        assert client.post(
            f"/team-activities/{activity_id}/participation-reports",
            json={"target_student_id": teammate["id"], "severity": "did_not_work", "comment": "Tarde"},
            headers=student_headers,
        ).status_code == 409

        _, _, other_director_headers = register(client, "director", "Other Director")
        other_school = client.post(
            "/institutions",
            json={"name": f"Other School {uuid4().hex[:10]}"},
            headers=other_director_headers,
        )
        assert other_school.status_code == 200
        conflict = client.post(
            "/institutions/attach-teacher",
            params={"email": teacher_email},
            headers=other_director_headers,
        )
        assert conflict.status_code == 409
