package co.edu.udea.motoscaribe.api.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Codigos de autorizacion de un solo uso y vida muy corta, atados al challenge PKCE con el que se
 * pidieron. Viven en memoria (una sola instancia). Un codigo se consume al primer intento de canje,
 * acierte o no el verifier, para que no se pueda adivinar por fuerza bruta.
 */
@Component
class AuthCodeStore {

	static final Duration TTL = Duration.ofSeconds(60);
	private static final int MAX_PENDING = 10_000;

	private record Pending(String document, String challenge, Instant expiresAt) {
	}

	private final ConcurrentHashMap<String, Pending> pending = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();

	/** Devuelve el codigo, o vacio si hay demasiados pendientes. */
	Optional<String> issue(String document, String challenge) {
		Instant now = Instant.now();
		pending.values().removeIf(p -> p.expiresAt().isBefore(now));
		if (pending.size() >= MAX_PENDING) {
			return Optional.empty();
		}
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		pending.put(code, new Pending(document, challenge, now.plus(TTL)));
		return Optional.of(code);
	}

	/** Canjea el codigo: devuelve la cedula solo si no vencio y el verifier corresponde al challenge. */
	Optional<String> redeem(String code, String verifier) {
		Pending p = pending.remove(code); // un solo intento
		if (p == null || p.expiresAt().isBefore(Instant.now()) || !Pkce.matches(p.challenge(), verifier)) {
			return Optional.empty();
		}
		return Optional.of(p.document());
	}
}
