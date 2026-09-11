from fastapi.responses import HTMLResponse, RedirectResponse

from .main import app
from .attendance_routes import router as attendance_router
from .evaluation_routes import router as evaluation_router
from .site import app_page, homepage, privacy_policy

app.include_router(attendance_router)
app.include_router(evaluation_router)

DOWNLOADS = {
    "profe": "https://github.com/yomismtz/ProfeCuaderno/releases/download/android-latest/ProfeCuaderno.apk",
    "estudiante": "https://github.com/yomismtz/El-Cuaderno-del-Estudiante-/releases/download/android-latest/El-Cuaderno-del-Estudiante.apk",
    "director": "https://github.com/yomismtz/El-escritorio-del-director/releases/download/android-latest/El-Escritorio-del-Director.apk",
}


@app.get("/", response_class=HTMLResponse, include_in_schema=False)
def public_homepage():
    return homepage()


@app.get("/profe", response_class=HTMLResponse, include_in_schema=False)
def public_teacher_page():
    return app_page("profe")


@app.get("/estudiante", response_class=HTMLResponse, include_in_schema=False)
def public_student_page():
    return app_page("estudiante")


@app.get("/director", response_class=HTMLResponse, include_in_schema=False)
def public_director_page():
    return app_page("director")


@app.get("/download/{app_key}", include_in_schema=False)
def download_app(app_key: str):
    target = DOWNLOADS.get(app_key)
    if target is None:
        return RedirectResponse(url="/", status_code=302)
    return RedirectResponse(url=target, status_code=307)


@app.get("/privacy", response_class=HTMLResponse, include_in_schema=False)
@app.get("/politica-de-privacidad", response_class=HTMLResponse, include_in_schema=False)
def public_privacy_policy():
    return privacy_policy()
