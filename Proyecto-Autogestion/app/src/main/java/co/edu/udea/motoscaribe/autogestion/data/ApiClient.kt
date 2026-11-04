package co.edu.udea.motoscaribe.autogestion.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** La API respondio con un codigo de error (401 credenciales, 429 bloqueo, 413 archivo grande...). */
class ApiException(val code: Int) : Exception("HTTP $code")

/** No hubo conexion (sin internet, servidor caido, tiempo agotado). */
class NetworkException(cause: Throwable) : Exception(cause)

/**
 * Cliente HTTP minimo sobre HttpURLConnection (sin dependencias extra). Guarda el token solo en
 * memoria: al cerrar la app hay que ingresar de nuevo. No registra cuerpos ni encabezados.
 */
class ApiClient(private val baseUrl: String) {

    @Volatile
    var token: String? = null

    /**
     * Si una consulta recibe 401, se llama una vez con el token que fallo; si devuelve true (renovo la
     * sesion) la consulta se repite con el token nuevo. No aplica a las rutas de autenticacion.
     */
    @Volatile
    var refresher: (suspend (failedToken: String?) -> Boolean)? = null

    suspend fun get(path: String): String = send("GET", path, null, null)

    suspend fun postJson(path: String, body: JSONObject): String =
        send("POST", path, "application/json; charset=utf-8", body.toString().toByteArray())

    suspend fun postFile(path: String, field: String, fileName: String, mime: String, bytes: ByteArray): String {
        val boundary = "----autogestion${UUID.randomUUID()}"
        val head = "--$boundary\r\nContent-Disposition: form-data; name=\"$field\"; filename=\"$fileName\"\r\n" +
            "Content-Type: $mime\r\n\r\n"
        val tail = "\r\n--$boundary--\r\n"
        val body = head.toByteArray() + bytes + tail.toByteArray()
        return send("POST", path, "multipart/form-data; boundary=$boundary", body)
    }

    private suspend fun send(method: String, path: String, contentType: String?, body: ByteArray?): String {
        val sentWith = token
        try {
            return execute(method, path, contentType, body)
        } catch (e: ApiException) {
            val renew = refresher
            if (e.code != 401 || sentWith == null || renew == null || path.startsWith("/api/v1/auth/")) throw e
            if (!renew(sentWith)) throw e
            return execute(method, path, contentType, body)
        }
    }

    private suspend fun execute(method: String, path: String, contentType: String?, body: ByteArray?): String =
        withContext(Dispatchers.IO) {
            val conn = try {
                URL(baseUrl + path).openConnection() as HttpURLConnection
            } catch (e: IOException) {
                throw NetworkException(e)
            }
            try {
                conn.requestMethod = method
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.setRequestProperty("Accept", "application/json")
                // Las rutas de autenticacion no llevan token (uno vencido haria fallar la renovacion).
                if (!path.startsWith("/api/v1/auth/")) token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", contentType)
                    conn.setFixedLengthStreamingMode(body.size)
                    conn.outputStream.use { it.write(body) }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) throw ApiException(code)
                text
            } catch (e: IOException) {
                throw NetworkException(e)
            } finally {
                conn.disconnect()
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000
        // El plan gratis de Azure duerme la API: el primer pedido puede tardar hasta ~1 min.
        const val READ_TIMEOUT_MS = 60_000
    }
}
