# ProfeCuaderno 📓

Aplicación Android para docentes y núcleo técnico del ecosistema escolar online formado por **ProfeCuaderno**, **El Cuaderno del Estudiante** y **El Escritorio del Director**.

## Descarga Android

La página pública del docente está disponible en:

https://profecuaderno-api-production.up.railway.app/profe

Descarga directa del APK instalable más reciente:

https://github.com/yomismtz/ProfeCuaderno/releases/download/android-latest/ProfeCuaderno.apk

El APK publicado por este flujo es una compilación de prueba para instalación directa. Android puede solicitar autorización para instalar aplicaciones desde el navegador o gestor de archivos.

## Modo docente

ProfeCuaderno conserva sus herramientas de trabajo docente y añade conexión opcional con el backend central para:

- Crear clases online y obtener un código de vinculación.
- Vincular estudiantes por su cuenta registrada, usando coincidencia exacta de correo cuando se sincronizan registros locales.
- Publicar avisos de clase.
- Sincronizar asistencia y calificaciones.
- Publicar formaciones de equipos ya creadas por el docente.
- Revisar coevaluaciones anónimas de participación y cerrar/consolidar actividades de equipo.
- Recibir comunicados privados de Dirección.

Las herramientas locales del cuaderno docente siguen siendo independientes del acceso online. La aplicación **La carpeta del docente offline** es otro producto y no forma parte de este repositorio ni de la política conjunta del ecosistema online.

## Ecosistema

- **ProfeCuaderno**: registra y administra la actividad académica de las clases del docente.
- **El Cuaderno del Estudiante**: permite al alumno consultar únicamente la información asociada a sus propias clases y enviar reportes de participación dentro de su equipo.
- **El Escritorio del Director**: administra la institución, vincula docentes, publica comunicados internos y organiza horarios sin abrir acceso general a calificaciones, asistencia o coevaluaciones.

Sitio público del ecosistema: https://profecuaderno-api-production.up.railway.app/

Política de privacidad conjunta: https://profecuaderno-api-production.up.railway.app/privacy

## Backend central

El backend FastAPI está en `backend/` y utiliza autenticación con token, permisos por rol y PostgreSQL en producción. Entre sus controles se incluyen pertenencia a institución/clase, límites de acceso del rol Director, validación de calificaciones y conflictos de horario, además de restricciones de privacidad para coevaluaciones.

La documentación interactiva de la API queda disponible en `/docs` y el estado del servicio en `/health`.

## Evaluación local

La aplicación mantiene rubros y rúbricas configurables, asistencia, periodos, equipos, reportes y exportaciones. Una calificación real de `0` se conserva como valor válido y no debe confundirse con datos ausentes.

## Compilación

El workflow `.github/workflows/build-apk.yml` ejecuta pruebas unitarias y lint, genera APK/AAB de prueba y actualiza una descarga pública estable del APK cuando cambia `main`.

Para desarrollo local, abre el repositorio en Android Studio con Java 17 y sincroniza Gradle.

## Privacidad

Los datos online se rigen por la política conjunta enlazada arriba. Los datos locales que el docente conserve en el dispositivo deben protegerse mediante los controles del dispositivo y las funciones de seguridad de la app. Antes de una publicación institucional definitiva deben confirmarse el responsable legal del tratamiento, un contacto estable de privacidad, los plazos de conservación y los proveedores de infraestructura vigentes.