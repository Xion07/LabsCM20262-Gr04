package co.edu.udea.motoscaribe.api.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Retos (challenge) de WebAuthn: aleatorios, de un solo uso y de corta vida. El servidor solo acepta una
 * firma si responde a un reto que el mismo emitio para ese proposito, asi una respuesta capturada no
 * se puede repetir.
 */
@Component
class ChallengeStore {

	enum Purpose { REGISTER, LOGIN }

	static final Duration TTL = Duration.ofMinutes(2);
	private static final int MAX_PENDING = 5_000;

	private record Entry(Purpose purpose, String document, Instant expiresAt) {
	}

	private final ConcurrentHashMap<String, Entry> pending = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();

	/** Emite un reto nuevo (32 bytes). document: la cedula que registra; vacio para el inicio de sesion. */
	byte[] issue(Purpose purpose, String document) {
		Instant now = Instant.now();
		pending.values().removeIf(e -> e.expiresAt().isBefore(now));
		if (pending.size() >= MAX_PENDING) {
			pending.clear();
		}
		byte[] challenge = new byte[32];
		random.nextBytes(challenge);
		pending.put(key(challenge), new Entry(purpose, document, now.plus(TTL)));
		return challenge;
	}

	/** Gasta el reto: devuelve la cedula asociada si existe, es del proposito pedido y no vencio. */
	Optional<String> consume(byte[] challenge, Purpose purpose) {
		Entry e = pending.remove(key(challenge));
		if (e == null || e.purpose() != purpose || e.expiresAt().isBefore(Instant.now())) {
			return Optional.empty();
		}
		return Optional.of(e.document());
	}

	private static String key(byte[] challenge) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(challenge);
	}
}
