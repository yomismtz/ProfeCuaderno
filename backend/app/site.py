from html import escape


def _page(title: str, body: str) -> str:
    return f"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="theme-color" content="#102A43">
<title>{escape(title)}</title>
<style>
:root{{--ink:#102A43;--muted:#52677b;--paper:#f7f9fc;--card:#fff;--line:#dce5ee;--accent:#1565c0;--accent2:#00796b;--warm:#8d5a00}}
*{{box-sizing:border-box}} body{{margin:0;font-family:Inter,system-ui,-apple-system,Segoe UI,Roboto,sans-serif;color:var(--ink);background:var(--paper);line-height:1.6}}
a{{color:var(--accent);text-decoration:none}} a:hover{{text-decoration:underline}}
header{{background:linear-gradient(135deg,#0b2239,#153e63);color:white;padding:72px 20px 60px}}
.wrap{{max-width:1080px;margin:auto}} .eyebrow{{letter-spacing:.11em;text-transform:uppercase;font-size:.78rem;font-weight:700;opacity:.8}}
h1{{font-size:clamp(2.2rem,7vw,4.7rem);line-height:1.02;margin:.35rem 0 1rem;max-width:900px}} h2{{font-size:clamp(1.55rem,3vw,2.25rem);margin:0 0 16px}} h3{{margin:.1rem 0 .5rem;font-size:1.15rem}}
.lead{{font-size:1.15rem;max-width:780px;color:#d8e8f6}} nav{{display:flex;gap:18px;flex-wrap:wrap;margin-top:24px}} nav a{{color:white;font-weight:650}}
main{{padding:46px 20px 72px}} section{{margin:0 auto 52px;max-width:1080px}} .grid{{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}}
.card{{background:var(--card);border:1px solid var(--line);border-radius:18px;padding:24px;box-shadow:0 8px 30px rgba(16,42,67,.06)}} .role{{display:inline-block;padding:5px 10px;border-radius:999px;background:#eaf2fb;font-size:.78rem;font-weight:700;margin-bottom:12px}}
.flow{{display:grid;grid-template-columns:1fr auto 1fr auto 1fr;align-items:center;gap:12px}} .node{{background:white;border:1px solid var(--line);border-radius:16px;padding:20px;text-align:center}} .arrow{{font-size:1.8rem;color:var(--muted)}}
.note{{border-left:4px solid var(--accent);background:#edf5fd;padding:14px 16px;border-radius:0 12px 12px 0}} .privacy{{max-width:900px}} .privacy h2{{margin-top:34px}} .privacy ul{{padding-left:22px}}
footer{{border-top:1px solid var(--line);padding:28px 20px;color:var(--muted);background:white}} .small{{font-size:.9rem;color:var(--muted)}}
@media(max-width:800px){{.grid{{grid-template-columns:1fr}}.flow{{grid-template-columns:1fr}}.arrow{{transform:rotate(90deg);justify-self:center}}header{{padding-top:52px}}}}
</style>
</head>
<body>{body}</body></html>"""


def homepage() -> str:
    body = """
<header><div class="wrap">
<div class="eyebrow">Ecosistema escolar conectado</div>
<h1>Tres aplicaciones, una misma comunidad escolar.</h1>
<p class="lead">ProfeCuaderno, El Cuaderno del Estudiante y El Escritorio del Director trabajan sobre un backend común para conectar la gestión docente, la consulta del alumno y la coordinación institucional sin mezclar permisos ni exponer información que cada rol no necesita.</p>
<nav><a href="#apps">Las 3 apps</a><a href="#flujo">Cómo se usan</a><a href="#importancia">Por qué importan</a><a href="/privacy">Política de privacidad</a><a href="/docs">API</a></nav>
</div></header>
<main>
<section id="apps"><h2>Las tres aplicaciones</h2><div class="grid">
<article class="card"><span class="role">DOCENTE</span><h3>ProfeCuaderno</h3><p>Espacio de trabajo del profesor. Organiza grupos, asistencia, evaluaciones, avisos, equipos y coevaluación. Vincula alumnos por su cuenta online y sincroniza los resultados pertinentes con el servicio central.</p><p><a href="https://github.com/yomismtz/ProfeCuaderno">Repositorio oficial →</a></p></article>
<article class="card"><span class="role">ESTUDIANTE</span><h3>El Cuaderno del Estudiante</h3><p>Vista personal y de solo lectura sobre la información académica que le corresponde al alumno: clases vinculadas, avisos, pendientes, asistencia, calificaciones, horario y progreso. También permite reportes de participación dentro de su propio equipo.</p><p><a href="https://github.com/yomismtz/El-Cuaderno-del-Estudiante-">Repositorio oficial →</a></p></article>
<article class="card"><span class="role">DIRECCIÓN</span><h3>El Escritorio del Director</h3><p>Coordina la institución, vincula docentes, consulta clases institucionales, publica comunicados privados para profesores y administra el horario. No abre acceso general a expedientes, calificaciones o coevaluaciones de alumnos.</p><p><a href="https://github.com/yomismtz/El-escritorio-del-director">Repositorio oficial →</a></p></article>
</div></section>
<section id="flujo"><h2>Cómo se relacionan</h2><div class="flow"><div class="node"><strong>Dirección</strong><br><span class="small">crea la institución, vincula docentes y organiza horarios</span></div><div class="arrow">→</div><div class="node"><strong>Docente</strong><br><span class="small">crea clases, comparte código, registra actividad académica</span></div><div class="arrow">→</div><div class="node"><strong>Estudiante</strong><br><span class="small">se une por código y consulta solo sus datos</span></div></div><p class="note" style="margin-top:18px">La relación no es una base abierta entre las tres apps: cada cuenta usa autenticación y permisos por rol. El backend central valida qué operación puede realizar cada tipo de usuario.</p></section>
<section id="importancia"><h2>Por qué este modelo es importante</h2><div class="grid"><article class="card"><h3>Una sola fuente de información</h3><p>Reduce la duplicación entre listas, avisos, horarios y calificaciones. Lo que se publica en el flujo online puede consultarse desde el rol correspondiente.</p></article><article class="card"><h3>Privacidad por función</h3><p>El director administra la institución sin entrar a funciones académicas reservadas al docente; el estudiante solo accede a sus clases y a su información.</p></article><article class="card"><h3>Trazabilidad escolar</h3><p>La separación entre roles permite que cada acción tenga un contexto claro: quién administra, quién registra y quién consulta.</p></article></div></section>
<section><h2>Uso recomendado</h2><p>1. Dirección crea o configura la institución y vincula a los docentes por su correo registrado. 2. El docente crea sus clases y entrega a cada grupo el código correspondiente. 3. El estudiante inicia sesión y usa ese código para incorporarse a la clase. 4. El docente registra y sincroniza la información académica. 5. El estudiante consulta sus datos; Dirección mantiene la coordinación institucional.</p><p class="small">La aplicación docente offline es un producto separado y no forma parte de este ecosistema online ni de esta política conjunta.</p></section>
</main><footer><div class="wrap">Ecosistema ProfeCuaderno · <a href="/privacy">Política de privacidad conjunta</a> · <a href="/health">Estado del servicio</a></div></footer>
"""
    return _page("Ecosistema ProfeCuaderno", body)


def privacy_policy() -> str:
    body = """
<header><div class="wrap"><div class="eyebrow">Privacidad</div><h1>Política de privacidad conjunta</h1><p class="lead">Aplica exclusivamente a ProfeCuaderno Online, El Cuaderno del Estudiante y El Escritorio del Director.</p><nav><a href="/">Volver al sitio</a><a href="#datos">Datos</a><a href="#derechos">Derechos</a></nav></div></header>
<main><section class="privacy">
<p><strong>Última actualización:</strong> 10 de septiembre de 2026.</p>
<p>Este ecosistema escolar utiliza un servicio central para permitir que docentes, estudiantes y personal directivo trabajen con información académica e institucional según su rol. Esta política describe de forma conjunta qué información puede tratarse, para qué se utiliza y qué límites de acceso existen.</p>
<h2 id="datos">1. Información que puede tratarse</h2>
<ul><li>Datos de cuenta: nombre, correo electrónico, rol de usuario y credenciales de acceso protegidas mediante hash en el servidor.</li><li>Vinculación institucional: institución, clases, docente responsable y pertenencia del estudiante a una clase.</li><li>Información académica: asistencia, calificaciones, actividades, avisos de clase y progreso derivado de los registros publicados por el docente.</li><li>Organización escolar: horarios, aulas o ubicaciones textuales de clase y comunicados institucionales.</li><li>Trabajo en equipo: composición de equipos, resultados de actividades y reportes de participación. Los reportes enviados por estudiantes se presentan al docente sin revelar públicamente la identidad del reportante.</li><li>Datos técnicos indispensables para autenticación y funcionamiento del servicio, como tokens de sesión almacenados por la aplicación.</li></ul>
<h2>2. Finalidades</h2><p>Los datos se usan para autenticar cuentas, vincular usuarios con su institución y sus clases, sincronizar información académica, mostrar avisos y horarios, permitir el seguimiento del progreso, facilitar la coordinación institucional y aplicar controles de acceso por rol.</p>
<h2>3. Separación de permisos</h2><p>El sistema limita el acceso según el rol. El docente administra las clases que le pertenecen. El estudiante consulta la información asociada a sus propias clases. Dirección administra la institución, los docentes y los horarios, pero no dispone de acceso general a listas de alumnos, asistencia, calificaciones ni detalle de coevaluaciones mediante las funciones reservadas al docente.</p>
<h2>4. Reportes de participación</h2><p>Un estudiante solo puede reportar a integrantes de su propio equipo en una actividad abierta. No puede reportarse a sí mismo ni seleccionar integrantes de otros equipos. El resumen entregado al docente no expone la identidad del estudiante que realizó el reporte. El docente debe revisar y resolver el reporte antes de usarlo como elemento de valoración.</p>
<h2>5. Compartición y venta de datos</h2><p>El ecosistema no está diseñado para vender información personal ni para usar información académica con fines publicitarios. Los datos se comparten internamente solo en la medida necesaria para la función escolar y los permisos de cada rol. El alojamiento técnico puede depender de proveedores de infraestructura necesarios para operar el backend y la base de datos.</p>
<h2>6. Conservación</h2><p>La información se conserva mientras sea necesaria para mantener las cuentas, clases e institución activas y para la continuidad del servicio escolar. El responsable de cada implementación debe definir los plazos administrativos concretos de conservación y respaldo antes de un despliegue institucional definitivo.</p>
<h2>7. Seguridad</h2><p>La comunicación con el servicio público se realiza mediante HTTPS. El acceso a endpoints privados requiere autenticación mediante token y el servidor aplica comprobaciones de rol y pertenencia a clases o institución. Ningún sistema puede garantizar riesgo cero, por lo que las credenciales no deben compartirse y los dispositivos deben mantenerse protegidos.</p>
<h2>8. Menores de edad</h2><p>El sistema puede utilizarse en contextos escolares con estudiantes menores de edad. La institución que despliegue las aplicaciones debe contar con la base jurídica, autorizaciones y avisos requeridos por la normativa aplicable para tratar datos de alumnos, y debe limitar la información capturada a la necesaria para fines educativos.</p>
<h2 id="derechos">9. Acceso, corrección y eliminación</h2><p>Los usuarios pueden solicitar acceso, corrección o eliminación de sus datos al responsable de la implementación escolar o al desarrollador indicado en la ficha oficial de distribución de las aplicaciones. Actualmente el ecosistema no ofrece un botón universal de eliminación automática de cuenta; las solicitudes deben tramitarse administrativamente y esta capacidad debe revisarse antes de una publicación comercial o institucional a gran escala.</p>
<h2>10. Transferencias y proveedor de infraestructura</h2><p>El backend puede alojarse en infraestructura en la nube. La ubicación física y las condiciones de tratamiento dependerán del proveedor y de la configuración elegida por quien opere el servicio. Para una adopción institucional, el responsable deberá documentar al proveedor vigente y sus condiciones contractuales.</p>
<h2>11. Cambios a esta política</h2><p>Esta política puede actualizarse cuando cambien las funciones, proveedores, datos tratados o requisitos legales. La fecha de actualización se mostrará al inicio de esta página.</p>
<h2>12. Alcance</h2><p>Esta política cubre las tres aplicaciones online del ecosistema. <strong>No cubre la aplicación docente offline</strong>, que permanece separada y no se conecta a este backend.</p>
<p class="note">Antes de publicar las aplicaciones en Google Play o desplegarlas formalmente en una institución, debe confirmarse el nombre legal del responsable, un correo de privacidad estable, los plazos de conservación y los proveedores de infraestructura vigentes.</p>
</section></main><footer><div class="wrap"><a href="/">Ecosistema ProfeCuaderno</a> · Política conjunta de las tres apps online</div></footer>
"""
    return _page("Política de privacidad · Ecosistema ProfeCuaderno", body)
