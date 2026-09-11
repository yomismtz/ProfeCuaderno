from html import escape


APPS = {
    "profe": {
        "role": "DOCENTE",
        "name": "ProfeCuaderno",
        "version": "1.9.1",
        "summary": "Gestión docente conectada para grupos, asistencia, evaluación, avisos, equipos y reportes.",
        "download": "/download/profe",
        "repo": "https://github.com/yomismtz/ProfeCuaderno",
        "features": [
            "Trabajo local con sincronización cuando vuelve la conexión.",
            "Asistencia con reglas configurables y sesiones por fecha.",
            "Rubros, rúbricas, actividades, exámenes y calificación ponderada.",
            "Avisos, equipos, coevaluación y comunicación con Dirección.",
        ],
    },
    "estudiante": {
        "role": "ESTUDIANTE",
        "name": "El Cuaderno del Estudiante",
        "version": "0.3.0",
        "summary": "Consulta personal de clases, asistencia, calificaciones, progreso, avisos y horario.",
        "download": "/download/estudiante",
        "repo": "https://github.com/yomismtz/El-Cuaderno-del-Estudiante-",
        "features": [
            "Consulta del último estado sincronizado incluso sin conexión.",
            "Calificaciones en escala 0–100 y porcentajes iguales a los del docente.",
            "Sin evaluar se mantiene separado de una calificación real de cero.",
            "Acceso únicamente a las clases y datos del propio estudiante.",
        ],
    },
    "director": {
        "role": "DIRECCIÓN",
        "name": "El Escritorio del Director",
        "version": "0.2.0",
        "summary": "Coordinación institucional de docentes, clases, comunicados y horarios con permisos separados.",
        "download": "/download/director",
        "repo": "https://github.com/yomismtz/El-escritorio-del-director",
        "features": [
            "Institución y docentes vinculados por cuenta registrada.",
            "Horario institucional con detección de conflictos.",
            "Comunicados privados para el personal docente.",
            "Vista institucional agregada sin abrir calificaciones individuales.",
        ],
    },
}


def _page(title: str, body: str) -> str:
    return f"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="theme-color" content="#102A43">
<meta name="description" content="Ecosistema ProfeCuaderno: aplicaciones Android para docentes, estudiantes y Dirección.">
<title>{escape(title)}</title>
<style>
:root{{--ink:#102a43;--muted:#52677b;--paper:#f5f8fc;--card:#fff;--line:#dce5ee;--accent:#1565c0;--accent2:#00796b;--warm:#8d5a00;--soft:#eaf2fb}}
*{{box-sizing:border-box}}html{{scroll-behavior:smooth}}body{{margin:0;font-family:Inter,system-ui,-apple-system,Segoe UI,Roboto,sans-serif;color:var(--ink);background:var(--paper);line-height:1.6}}
a{{color:var(--accent);text-decoration:none}}a:hover{{text-decoration:underline}}
header{{background:linear-gradient(135deg,#0b2239,#153e63 58%,#176b6b);color:white;padding:70px 20px 58px}}.wrap{{max-width:1100px;margin:auto}}
.eyebrow{{letter-spacing:.12em;text-transform:uppercase;font-size:.78rem;font-weight:800;opacity:.82}}h1{{font-size:clamp(2.25rem,7vw,4.8rem);line-height:1.02;margin:.35rem 0 1rem;max-width:900px}}h2{{font-size:clamp(1.55rem,3vw,2.25rem);margin:0 0 16px}}h3{{margin:.1rem 0 .55rem;font-size:1.18rem}}
.lead{{font-size:1.15rem;max-width:800px;color:#d8e8f6}}nav{{display:flex;gap:16px;flex-wrap:wrap;margin-top:24px}}nav a{{color:white;font-weight:700}}
main{{padding:44px 20px 72px}}section{{margin:0 auto 52px;max-width:1100px}}.grid{{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}}.grid.two{{grid-template-columns:repeat(2,minmax(0,1fr))}}
.card{{background:var(--card);border:1px solid var(--line);border-radius:20px;padding:24px;box-shadow:0 8px 30px rgba(16,42,67,.06)}}.role{{display:inline-block;padding:5px 10px;border-radius:999px;background:var(--soft);font-size:.78rem;font-weight:800;margin-bottom:12px}}.version{{font-size:.82rem;color:var(--muted);font-weight:700}}
.actions{{display:flex;gap:10px;flex-wrap:wrap;margin-top:18px}}.btn{{display:inline-flex;align-items:center;justify-content:center;min-height:48px;padding:11px 17px;border-radius:12px;font-weight:800;text-decoration:none!important;border:1px solid transparent}}.btn.primary{{background:var(--accent);color:white}}.btn.secondary{{background:white;border-color:var(--line);color:var(--ink)}}
.note{{border-left:4px solid var(--accent);background:#edf5fd;padding:14px 16px;border-radius:0 12px 12px 0}}.install{{background:#fff7e7;border-left-color:#b36b00}}ul.clean{{padding-left:20px;margin-bottom:0}}.small{{font-size:.9rem;color:var(--muted)}}.privacy{{max-width:900px}}.privacy h2{{margin-top:34px}}
.flow{{display:grid;grid-template-columns:1fr auto 1fr auto 1fr;align-items:center;gap:12px}}.node{{background:white;border:1px solid var(--line);border-radius:16px;padding:20px;text-align:center}}.arrow{{font-size:1.7rem;color:var(--muted)}}footer{{border-top:1px solid var(--line);padding:28px 20px;color:var(--muted);background:white}}
@media(max-width:800px){{.grid,.grid.two{{grid-template-columns:1fr}}.flow{{grid-template-columns:1fr}}.arrow{{transform:rotate(90deg);justify-self:center}}header{{padding-top:50px}}}}
</style>
</head><body>{body}</body></html>"""


def _app_card(key: str) -> str:
    app = APPS[key]
    return f"""<article class="card"><span class="role">{app['role']}</span><div class="version">Versión {app['version']}</div><h3>{app['name']}</h3><p>{app['summary']}</p><div class="actions"><a class="btn primary" href="{app['download']}">⬇ Descargar APK</a><a class="btn secondary" href="/{key}">Ver la app</a></div></article>"""


def homepage() -> str:
    cards = "".join(_app_card(key) for key in ("profe", "estudiante", "director"))
    body = f"""
<header><div class="wrap"><div class="eyebrow">Ecosistema escolar conectado</div><h1>Tres apps. Un mismo flujo escolar.</h1><p class="lead">Descarga ProfeCuaderno, El Cuaderno del Estudiante y El Escritorio del Director desde un solo sitio. Cada aplicación conserva permisos específicos y sincroniza únicamente la información que corresponde a su rol.</p><nav><a href="#descargas">Descargas</a><a href="#flujo">Cómo funciona</a><a href="/privacy">Privacidad</a><a href="/health">Estado del servicio</a></nav></div></header>
<main><section id="descargas"><h2>Descargar las aplicaciones</h2><div class="grid">{cards}</div><p class="note install" style="margin-top:18px"><strong>Instalación directa:</strong> estos APK son compilaciones de prueba firmadas para instalarse en Android. El teléfono puede pedir permiso para instalar aplicaciones desde el navegador o gestor de archivos. No es necesario descargar el AAB.</p></section>
<section id="flujo"><h2>Cómo se relacionan</h2><div class="flow"><div class="node"><strong>Dirección</strong><br><span class="small">institución, docentes, horarios y comunicados</span></div><div class="arrow">→</div><div class="node"><strong>Docente</strong><br><span class="small">clases, asistencia, evaluación y equipos</span></div><div class="arrow">→</div><div class="node"><strong>Estudiante</strong><br><span class="small">consulta sus propios datos y progreso</span></div></div><p class="note" style="margin-top:18px">Las tres apps usan el mismo backend, pero no una base abierta: el servidor valida rol, institución y pertenencia a clase en cada operación.</p></section>
<section><h2>Diseñado para seguir trabajando</h2><div class="grid"><article class="card"><h3>Offline-first</h3><p>Las apps conservan información local útil y sincronizan cuando vuelve la conexión, según las funciones de cada rol.</p></article><article class="card"><h3>Privacidad por función</h3><p>Dirección coordina la institución sin convertirse en una vista de calificaciones individuales; el alumno consulta solo sus datos.</p></article><article class="card"><h3>Descarga siempre actualizada</h3><p>Cada cambio aprobado en la rama principal genera un APK y reemplaza automáticamente el archivo publicado para descarga.</p></article></div></section></main>
<footer><div class="wrap">Ecosistema ProfeCuaderno · <a href="/privacy">Política de privacidad</a> · <a href="/docs">API</a></div></footer>"""
    return _page("Ecosistema ProfeCuaderno · Descargas", body)


def app_page(key: str) -> str:
    app = APPS[key]
    features = "".join(f"<li>{item}</li>" for item in app["features"])
    body = f"""
<header><div class="wrap"><div class="eyebrow">{app['role']} · Versión {app['version']}</div><h1>{app['name']}</h1><p class="lead">{app['summary']}</p><div class="actions"><a class="btn primary" href="{app['download']}">⬇ Descargar APK Android</a><a class="btn secondary" href="{app['repo']}" style="color:#102a43">Código y versiones</a></div><nav><a href="/">← Las 3 apps</a><a href="#funciones">Funciones</a><a href="#instalar">Instalación</a><a href="/privacy">Privacidad</a></nav></div></header>
<main><section id="funciones"><div class="grid two"><article class="card"><h2>Qué incluye</h2><ul class="clean">{features}</ul></article><article class="card"><h2>Conexión con el ecosistema</h2><p>La información online se comparte únicamente cuando corresponde al rol y a la relación institucional o de clase. La app conserva sus datos locales disponibles según su función y vuelve a sincronizar cuando existe conexión.</p></article></div></section>
<section id="instalar"><h2>Cómo instalar</h2><div class="card"><p><strong>1.</strong> Pulsa “Descargar APK Android”. <strong>2.</strong> Abre el archivo descargado. <strong>3.</strong> Si Android lo solicita, autoriza temporalmente la instalación desde esa fuente. <strong>4.</strong> Instala o actualiza la app.</p><p class="note install">Esta descarga es una compilación de prueba para instalación directa. Si posteriormente se distribuye una versión oficial mediante Google Play, la ficha de la tienda será la vía recomendada para usuarios finales.</p></div></section></main>
<footer><div class="wrap"><a href="/">Ecosistema ProfeCuaderno</a> · <a href="/privacy">Privacidad</a> · <a href="{app['repo']}">Repositorio</a></div></footer>"""
    return _page(f"{app['name']} · Descargar", body)


def privacy_policy() -> str:
    body = """
<header><div class="wrap"><div class="eyebrow">Privacidad</div><h1>Política de privacidad conjunta</h1><p class="lead">Aplica exclusivamente a ProfeCuaderno Online, El Cuaderno del Estudiante y El Escritorio del Director.</p><nav><a href="/">Volver al sitio</a><a href="#datos">Datos</a><a href="#derechos">Derechos</a></nav></div></header>
<main><section class="privacy"><p><strong>Última actualización:</strong> 11 de septiembre de 2026.</p><p>Este ecosistema escolar utiliza un servicio central para permitir que docentes, estudiantes y personal directivo trabajen con información académica e institucional según su rol.</p>
<h2 id="datos">1. Información que puede tratarse</h2><ul><li>Datos de cuenta: nombre, correo electrónico, rol y credenciales protegidas mediante hash.</li><li>Vinculación institucional: institución, clases, docente responsable y pertenencia del estudiante.</li><li>Información académica: asistencia, calificaciones, actividades, avisos y progreso.</li><li>Organización escolar: horarios, aulas o ubicaciones textuales y comunicados.</li><li>Trabajo en equipo: composición, resultados y reportes de participación.</li><li>Datos técnicos indispensables para autenticación y funcionamiento.</li></ul>
<h2>2. Finalidades</h2><p>Los datos se usan para autenticar cuentas, vincular usuarios con su institución y clases, sincronizar información académica, mostrar avisos y horarios, facilitar el seguimiento del progreso y aplicar controles de acceso por rol.</p>
<h2>3. Separación de permisos</h2><p>El docente administra sus clases. El estudiante consulta la información asociada a sus propias clases. Dirección administra institución, docentes y horarios, pero no dispone de acceso general a calificaciones individuales ni detalle de coevaluaciones mediante las funciones reservadas al docente.</p>
<h2>4. Reportes de participación</h2><p>Un estudiante solo puede reportar a integrantes de su propio equipo en una actividad abierta. El docente debe revisar y resolver el reporte antes de usarlo como elemento de valoración.</p>
<h2>5. Compartición y venta de datos</h2><p>El ecosistema no está diseñado para vender información personal ni usar información académica con fines publicitarios. El alojamiento técnico puede depender de proveedores de infraestructura necesarios para operar el servicio.</p>
<h2>6. Conservación</h2><p>La información se conserva mientras sea necesaria para mantener las cuentas, clases e institución activas y para la continuidad del servicio escolar.</p>
<h2>7. Seguridad</h2><p>La comunicación pública se realiza mediante HTTPS. Los endpoints privados requieren autenticación mediante token y el servidor aplica comprobaciones de rol y pertenencia.</p>
<h2>8. Menores de edad</h2><p>La institución que despliegue las aplicaciones debe contar con la base jurídica, autorizaciones y avisos requeridos por la normativa aplicable para tratar datos de alumnos.</p>
<h2 id="derechos">9. Acceso, corrección y eliminación</h2><p>Los usuarios pueden solicitar acceso, corrección o eliminación de sus datos al responsable de la implementación escolar o al desarrollador indicado en la ficha oficial de distribución.</p>
<h2>10. Infraestructura</h2><p>El backend puede alojarse en infraestructura en la nube. Para una adopción institucional, el responsable deberá documentar al proveedor vigente y sus condiciones contractuales.</p>
<h2>11. Cambios</h2><p>Esta política puede actualizarse cuando cambien las funciones, proveedores, datos tratados o requisitos legales.</p>
<h2>12. Alcance</h2><p>Esta política cubre las tres aplicaciones online del ecosistema. <strong>No cubre la aplicación docente offline</strong>, que permanece separada y no se conecta a este backend.</p></section></main>
<footer><div class="wrap"><a href="/">Ecosistema ProfeCuaderno</a> · Política conjunta de las tres apps online</div></footer>"""
    return _page("Política de privacidad · Ecosistema ProfeCuaderno", body)
