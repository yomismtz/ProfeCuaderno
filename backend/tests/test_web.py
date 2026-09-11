import os

os.environ["DATABASE_URL"] = "sqlite:///./test_backend.db"
os.environ["JWT_SECRET"] = "test-secret"

from fastapi.testclient import TestClient
from app.web import app


def test_public_ecosystem_homepage():
    with TestClient(app) as client:
        response = client.get("/")
        assert response.status_code == 200
        assert "text/html" in response.headers["content-type"]
        assert "ProfeCuaderno" in response.text
        assert "El Cuaderno del Estudiante" in response.text
        assert "El Escritorio del Director" in response.text
        assert "Política de privacidad" in response.text


def test_joint_privacy_policy_and_alias():
    with TestClient(app) as client:
        response = client.get("/privacy")
        assert response.status_code == 200
        assert "Política de privacidad conjunta" in response.text
        assert "No cubre la aplicación docente offline" in response.text

        alias = client.get("/politica-de-privacidad")
        assert alias.status_code == 200
        assert "Política de privacidad conjunta" in alias.text
