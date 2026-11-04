package co.edu.udea.motoscaribe.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/** PKCE (RFC 7636), metodo S256: challenge = BASE64URL(SHA-256(verifier)), sin relleno. */
final class Pkce {

	private Pkce() {
	}

	static String challengeOf(String verifier) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e); // SHA-256 siempre existe en la JVM
		}
	}

	/** Comparacion en tiempo constante entre el challenge guardado y el del verifier recibido. */
	static boolean matches(String challenge, String verifier) {
		return MessageDigest.isEqual(challenge.getBytes(StandardCharsets.US_ASCII),
				challengeOf(verifier).getBytes(StandardCharsets.US_ASCII));
	}
}
