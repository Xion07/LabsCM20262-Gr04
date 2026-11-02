package co.edu.udea.motoscaribe.api.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Pagina publica de la raiz: sin ella, abrir la API en el navegador solo muestra un 401 en blanco. */
@RestController
class HomeController {

	private static final String PAGE = """
			<!doctype html>
			<html lang="es"><head><meta charset="utf-8">
			<meta name="viewport" content="width=device-width,initial-scale=1">
			<title>API de autogestión · Motos del Caribe</title>
			<style>
			body{font-family:system-ui,sans-serif;background:#2b1d1f;color:#fff5f3;margin:0;padding:24px;line-height:1.5}
			main{max-width:720px;margin:0 auto}
			h1{margin:0 0 4px}.ok{color:#7be0a6;font-weight:600}
			code,pre{background:#3b2a2d;border-radius:8px;padding:2px 6px}
			pre{padding:12px;overflow:auto}
			li{margin:4px 0}a{color:#ff8fa3}
			</style></head><body><main>
			<h1>API de autogestión</h1>
			<p class="ok">● En línea</p>
			<p>API REST de la app móvil de autogestión de crédito de Motos del Caribe. Proyecto de aula de la
			Universidad de Antioquia con <b>datos ficticios</b>. Todo exige un token salvo el inicio de sesión,
			por eso abrir un endpoint en el navegador responde 401: es lo esperado.</p>
			<h2>Probarla</h2>
			<p>Cédulas de demostración <code>1000000001</code> a <code>1000000020</code>, clave <code>demo1234</code>.</p>
			<pre>curl -X POST https://autogestion-api-jc.azurewebsites.net/api/v1/auth/login \\
			  -H "Content-Type: application/json" \\
			  -d '{"document":"1000000001","password":"demo1234"}'</pre>
			<p>La respuesta trae un <code>accessToken</code>; se envía como <code>Authorization: Bearer &lt;token&gt;</code>.</p>
			<h2>Endpoints</h2>
			<ul>
			<li><code>GET /api/v1/me/contract</code>: saldo, próxima cuota, plazo y km</li>
			<li><code>GET /api/v1/me/payments</code>: abonos</li>
			<li><code>GET /api/v1/me/fines</code>: multas</li>
			<li><code>GET /api/v1/me/km-history</code>: kilometraje</li>
			<li><code>GET /api/v1/me/extension-requests</code>: historial de solicitudes de plazo</li>
			<li><code>POST /api/v1/me/extension-requests</code>: pedir más plazo</li>
			</ul>
			<p>La primera consulta tras un rato sin uso puede tardar hasta un minuto (plan gratis de Azure).
			Código y documentación: <a href="https://github.com/Xion07/LabsCM20262-Gr04">github.com/Xion07/LabsCM20262-Gr04</a>.</p>
			</main></body></html>
			""";

	@GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
	String home() {
		return PAGE;
	}
}
