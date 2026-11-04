package co.edu.udea.motoscaribe.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Refresh tokens opacos con rotacion: cada uso invalida el token y entrega uno nuevo. Se guarda solo
 * el hash SHA-256 (si se filtrara la memoria no se podrian usar). Viven en memoria: se pierden al
 * reiniciar la API y el cliente simplemente vuelve a iniciar sesion.
 */
@Component
class RefreshTokenStore {

	static final Duration TTL = Duration.ofDays(7);
	private static final int MAX_ACTIVE = 10_000;

	private record Entry(String document, Instant expiresAt) {
	}

	private final ConcurrentHashMap<String, Entry> active = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();

	/** Emite un refresh token nuevo para la cedula. */
	String issue(String document) {
		Instant now = Instant.now();
		active.values().removeIf(e -> e.expiresAt().isBefore(now));
		if (active.size() >= MAX_ACTIVE) {
			active.clear(); // proteccion de memoria en una demo: obliga a iniciar sesion de nuevo
		}
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		active.put(hash(token), new Entry(document, now.plus(TTL)));
		return token;
	}

	/** Consume el token y devuelve la cedula; el llamador debe emitir uno nuevo. */
	Optional<String> consume(String token) {
		Entry e = active.remove(hash(token));
		if (e == null || e.expiresAt().isBefore(Instant.now())) {
			return Optional.empty();
		}
		return Optional.of(e.document());
	}

	/** Cierra la sesion: invalida el token si existe. */
	void revoke(String token) {
		active.remove(hash(token));
	}

	private static String hash(String token) {
		try {
			return HexFormat.of().formatHex(
					MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII)));
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
