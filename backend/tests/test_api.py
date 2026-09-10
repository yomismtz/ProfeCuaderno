import os
os.environ["DATABASE_URL"] = "sqlite:///./test_backend.db"
os.environ["JWT_SECRET"] = "test-secret"

from fastapi.testclient import TestClient
from app.main import app


def test_health():
    with TestClient(app) as client:
        response = client.get("/health")
        assert response.status_code == 200
        assert response.json()["status"] == "ok"


def test_register_and_login_student():
    with TestClient(app) as client:
        email = "student-test@example.com"
        payload = {"email": email, "password": "password123", "full_name": "Student Test", "role": "student"}
        response = client.post("/auth/register", json=payload)
        if response.status_code == 409:
            response = client.post("/auth/login", json={"email": email, "password": "password123"})
        assert response.status_code == 200
        assert response.json()["user"]["role"] == "student"
