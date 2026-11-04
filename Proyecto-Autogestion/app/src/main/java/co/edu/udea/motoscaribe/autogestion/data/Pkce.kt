package co.edu.udea.motoscaribe.autogestion.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * PKCE (RFC 7636, metodo S256). El "verifier" es un secreto aleatorio de un solo inicio de sesion que
 * nunca sale de la app hasta el canje; al servidor primero solo le llega su huella (el "challenge").
 */
object Pkce {
    private val urlEncoder = Base64.getUrlEncoder().withoutPadding()

    /** 32 bytes aleatorios en base64url: 43 caracteres, dentro de lo que pide el RFC (43 a 128). */
    fun newVerifier(): String = urlEncoder.encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })

    /** BASE64URL(SHA-256(verifier)) sin relleno. */
    fun challengeOf(verifier: String): String =
        urlEncoder.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
}
