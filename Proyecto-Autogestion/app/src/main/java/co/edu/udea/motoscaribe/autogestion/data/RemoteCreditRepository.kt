package co.edu.udea.motoscaribe.autogestion.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/** Implementacion que habla con la API REST de autogestion. */
class RemoteCreditRepository(
    private val context: Context,
    private val api: ApiClient,
) : CreditRepository {

    /** Solo en memoria, como el access token: al cerrar la app hay que volver a ingresar. */
    @Volatile
    private var refreshToken: String? = null
    private val refreshLock = Mutex()

    init {
        api.refresher = ::renewSession
    }

    /** Inicio de sesion con PKCE: pide un codigo con la huella del secreto y lo canjea mostrando el secreto. */
    override suspend fun login(document: String, password: String) {
        api.token = null
        refreshToken = null
        val verifier = Pkce.newVerifier()
        val authorize = JSONObject()
            .put("document", document).put("password", password)
            .put("codeChallenge", Pkce.challengeOf(verifier)).put("codeChallengeMethod", "S256")
        val code = JsonMapping.authCode(api.postJson("/api/v1/auth/authorize", authorize))
        val tokens = api.postJson("/api/v1/auth/token", JSONObject().put("code", code).put("codeVerifier", verifier))
        storeTokens(tokens)
    }

    override fun logout() {
        val refresh = refreshToken
        api.token = null
        refreshToken = null
        if (refresh != null) {
            // Avisa al servidor para que el refresh token deje de servir; si falla no importa.
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching { api.postJson("/api/v1/auth/logout", JSONObject().put("refreshToken", refresh)) }
            }
        }
    }

    private fun storeTokens(json: String) {
        api.token = JsonMapping.accessToken(json)
        refreshToken = JsonMapping.refreshToken(json)
    }

    /** Cambia el refresh token por tokens nuevos. true si renovo; false si hay que volver a ingresar. */
    private suspend fun renewSession(failedToken: String?): Boolean = refreshLock.withLock {
        // Otra consulta ya renovo mientras esperabamos el candado: basta con reintentar.
        if (api.token != failedToken) return@withLock true
        val refresh = refreshToken ?: return@withLock false
        try {
            storeTokens(api.postJson("/api/v1/auth/refresh", JSONObject().put("refreshToken", refresh)))
            true
        } catch (e: ApiException) {
            refreshToken = null
            false
        }
    }

    override suspend fun contract() = JsonMapping.contract(api.get("/api/v1/me/contract"))

    override suspend fun payments() = JsonMapping.payments(api.get("/api/v1/me/payments"))

    override suspend fun fines() = JsonMapping.fines(api.get("/api/v1/me/fines"))

    override suspend fun kmHistory() = JsonMapping.kmHistory(api.get("/api/v1/me/km-history"))

    override suspend fun extensions() = JsonMapping.extensions(api.get("/api/v1/me/extension-requests"))

    override suspend fun requestExtension(request: ExtensionRequest) {
        val id = JsonMapping.extensionId(
            api.postJson("/api/v1/me/extension-requests", JsonMapping.extensionBody(request))
        )
        request.attachmentUri?.let { uploadAttachment(id, Uri.parse(it)) }
    }

    private suspend fun uploadAttachment(requestId: String, uri: Uri) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val bytes = resolver.openInputStream(uri)?.use { it.readNBytesCapped(MAX_ATTACHMENT_BYTES + 1) }
            ?: throw NetworkException(java.io.FileNotFoundException())
        if (bytes.size > MAX_ATTACHMENT_BYTES) throw ApiException(413)
        api.postFile("/api/v1/me/extension-requests/$requestId/attachment", "file", "soporte", mime, bytes)
    }

    private fun java.io.InputStream.readNBytesCapped(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (out.size() < limit) {
            val n = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_ATTACHMENT_BYTES = 5 * 1024 * 1024 // el mismo limite de la API
    }
}
