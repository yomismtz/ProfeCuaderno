import os

os.environ["DATABASE_URL"] = "sqlite:///./test_backend.db"
os.environ["JWT_SECRET"] = "test-secret"

from fastapi.testclient import TestClient
from app.web import app


def test_public_app_pages_and_download_redirects():
    with TestClient(app) as client:
        for path, expected in (
            ("/profe", "ProfeCuaderno"),
            ("/estudiante", "El Cuaderno del Estudiante"),
            ("/director", "El Escritorio del Director"),
        ):
            response = client.get(path)
            assert response.status_code == 200
            assert expected in response.text
            assert "Descargar APK Android" in response.text

        expected_downloads = {
            "/download/profe": "https://github.com/yomismtz/ProfeCuaderno/releases/download/android-latest/ProfeCuaderno.apk",
            "/download/estudiante": "https://github.com/yomismtz/El-Cuaderno-del-Estudiante-/releases/download/android-latest/El-Cuaderno-del-Estudiante.apk",
            "/download/director": "https://github.com/yomismtz/El-escritorio-del-director/releases/download/android-latest/El-Escritorio-del-Director.apk",
        }
        for path, expected in expected_downloads.items():
            response = client.get(path, follow_redirects=False)
            assert response.status_code == 307
            assert response.headers["location"] == expected


def test_homepage_exposes_all_three_downloads():
    with TestClient(app) as client:
        response = client.get("/")
        assert response.status_code == 200
        assert "/download/profe" in response.text
        assert "/download/estudiante" in response.text
        assert "/download/director" in response.text
