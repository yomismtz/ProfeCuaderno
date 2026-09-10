# Backend central de La Carpeta del Docente

Servicio central para conectar **Docente Online**, **El Cuaderno del Estudiante** y **El Escritorio del Director**. La versión **Docente Offline queda deliberadamente fuera de este backend**.

## Qué incluye

- Autenticación JWT para docente, estudiante y director.
- Instituciones y vinculación de docentes.
- Clases con código de acceso para alumnos.
- Avisos de clase.
- Asistencia.
- Calificaciones por rubro/actividad.
- Actividades por equipos con calificación grupal y ajustes individuales.
- Cierre y consolidación de calificaciones del equipo al alumno.
- Reportes anónimos de participación entre miembros del mismo equipo.
- Resumen anónimo para el docente, sin exponer quién reportó.
- Revisión docente: confirmado, no confirmado, participación parcial o evidencia insuficiente.
- Horarios institucionales con detección de conflicto de docente y salón.

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

## Integración Android

Las apps Online deben enviar `Authorization: Bearer <token>`. Los endpoints principales son:

- `POST /auth/register`, `POST /auth/login`, `GET /me`
- `POST /classes`, `POST /classes/join`, `GET /classes`
- `GET/POST /classes/{id}/notices`
- `PUT /classes/{id}/attendance`, `GET /classes/{id}/attendance/me`
- `PUT /classes/{id}/grades`, `GET /classes/{id}/grades/me`
- `POST/GET /classes/{id}/team-activities`
- `PUT /team-activities/{id}/scores`
- `POST /team-activities/{id}/participation-reports`
- `GET /team-activities/{id}/participation-summary`
- `PUT /team-activities/{id}/participation-review/{student_id}`
- `POST/GET /schedule`

## Producción

El código está preparado para PostgreSQL y contenedor Docker. Para convertirlo en un backend público real todavía se debe desplegar el contenedor en un proveedor y configurar `DATABASE_URL`, `JWT_SECRET` y las credenciales iniciales del director. No se deben guardar secretos reales en GitHub.
