package co.edu.udea.motoscaribe.autogestion.data

import android.content.Context
import android.net.Uri

/** Implementacion que habla con la API REST de autogestion. */
class RemoteCreditRepository(
    private val context: Context,
    private val api: ApiClient,
) : CreditRepository {

    override suspend fun login(document: String, password: String) {
        val body = org.json.JSONObject().put("document", document).put("password", password)
        api.token = null
        api.token = JsonMapping.accessToken(api.postJson("/api/v1/auth/login", body))
    }

    override fun logout() {
        api.token = null
    }

    override suspend fun contract() = JsonMapping.contract(api.get("/api/v1/me/contract"))

    override suspend fun payments() = JsonMapping.payments(api.get("/api/v1/me/payments"))

    override suspend fun fines() = JsonMapping.fines(api.get("/api/v1/me/fines"))

    override suspend fun kmHistory() = JsonMapping.kmHistory(api.get("/api/v1/me/km-history"))

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
