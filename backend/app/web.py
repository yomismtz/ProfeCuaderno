from fastapi.responses import HTMLResponse

from .main import app
from .attendance_routes import router as attendance_router
from .site import homepage, privacy_policy

app.include_router(attendance_router)


@app.get("/", response_class=HTMLResponse, include_in_schema=False)
def public_homepage():
    return homepage()


@app.get("/privacy", response_class=HTMLResponse, include_in_schema=False)
@app.get("/politica-de-privacidad", response_class=HTMLResponse, include_in_schema=False)
def public_privacy_policy():
    return privacy_policy()
