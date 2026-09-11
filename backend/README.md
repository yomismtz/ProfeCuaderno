# Backend central de La Carpeta del Docente

Servicio central para conectar **Docente Online**, **El Cuaderno del Estudiante** y **El Escritorio del Director**. La versión **Docente Offline queda deliberadamente fuera de este backend**.

## Qué incluye

- Autenticación JWT para docente, estudiante y director.
- Instituciones y vinculación de docentes.
- Clases con código de acceso para alumnos.
- Avisos de clase.
- Asistencia con reglas configurables, títulos de sesión y días trabajados/no trabajados.
- Calificaciones por rubro/actividad.
- Actividades por equipos con calificación grupal y ajustes individuales.
- Cierre y consolidación de calificaciones del equipo al alumno.
- Reportes anónimos de participación entre miembros del mismo equipo.
- Resumen anónimo para el docente, sin exponer quién reportó.
- Revisión docente: confirmado, no confirmado, participación parcial o evidencia insuficiente.
- Horarios institucionales con detección de conflicto de docente y salón.
- Resúmenes de asistencia para alumno y panoramas institucionales agregados para Dirección.

## Ejecutar localmente

```bash
cd backend
cp .env.example .env
docker compose up --build
```

La API queda en `http://localhost:8000` y la documentación interactiva en `/docs`.

## Director inicial

Para producción define estas variables en el servidor:

```env
BOOTSTRAP_DIRECTOR_EMAIL=director@escuela.mx
BOOTSTRAP_DIRECTOR_PASSWORD=una-clave-larga
JWT_SECRET=un-secreto-aleatorio-muy-largo
DATABASE_URL=postgresql+psycopg://usuario:clave@host:5432/base
```

El director inicial se crea al arrancar si el correo todavía no existe.

## Privacidad de reportes anónimos

El servidor conserva internamente el identificador del alumno que reporta únicamente para impedir votos repetidos y verificar que pertenezca al mismo equipo. Los endpoints del docente **nunca devuelven la identidad del reportante**. Un solo reporte se marca como `requires_corroboration=true` y ningún reporte modifica automáticamente una calificación.

Dirección recibe únicamente información institucional y resúmenes agregados por grupo. Los endpoints institucionales de asistencia no exponen nombres, matrículas, calificaciones, historiales individuales ni coevaluaciones de estudiantes.

## Integración Android

Las apps Online deben enviar `Authorization: Bearer <token>`. Los endpoints principales son:

- `POST /auth/register`, `POST /auth/login`, `GET /me`
- `POST /classes`, `POST /classes/join`, `GET /classes`
- `GET/POST /classes/{id}/notices`
- `PUT /classes/{id}/attendance`, `GET /classes/{id}/attendance/me`
- `GET/PUT /classes/{id}/attendance-policy`
- `PUT /classes/{id}/attendance-session`, `GET /classes/{id}/attendance-sessions`
- `GET /classes/{id}/attendance-summary/me`
- `GET /classes/{id}/attendance-records/{student_id}`
- `GET /classes/{id}/attendance-summary`
- `GET /institutions/attendance-summary`
- `PUT /classes/{id}/grades`, `GET /classes/{id}/grades/me`
- `POST/GET /classes/{id}/team-activities`
- `PUT /team-activities/{id}/scores`
- `POST /team-activities/{id}/participation-reports`
- `GET /team-activities/{id}/participation-summary`
- `PUT /team-activities/{id}/participation-review/{student_id}`
- `POST/GET /schedule`

## Funcionamiento offline-first

Las tres aplicaciones Android Online están diseñadas para seguir siendo útiles sin conexión. El docente conserva su base SQLite como fuente local; Alumno conserva el último estado recibido y sus operaciones pendientes; Dirección mantiene cachés institucionales y una cola de cambios administrativos. Las operaciones que requieren servidor se reintentan automáticamente mediante trabajo en segundo plano cuando vuelve una conexión de red.

El backend continúa siendo la fuente compartida una vez sincronizados los cambios. Las migraciones añadidas para políticas y metadatos de sesiones son aditivas y no destruyen los registros anteriores.

## Producción

El servicio está preparado para PostgreSQL y se despliega desde la carpeta `backend` del repositorio. En producción deben configurarse `DATABASE_URL`, `JWT_SECRET` y las credenciales iniciales necesarias mediante variables del proveedor; no se deben guardar secretos reales en GitHub.
