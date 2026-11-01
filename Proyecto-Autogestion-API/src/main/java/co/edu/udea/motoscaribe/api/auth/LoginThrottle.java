package co.edu.udea.motoscaribe.api.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import co.edu.udea.motoscaribe.api.config.ApiProperties;

import org.springframework.stereotype.Component;

/**
 * Bloquea una cedula temporalmente tras varios intentos fallidos. Vive en memoria:
 * sirve para una sola instancia; con varias habria que moverlo a una BD compartida.
 */
@Component
class LoginThrottle {

	private record State(int failures, Instant lockedUntil) {
	}

	private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
	private final int maxAttempts;
	private final Duration lock;

	LoginThrottle(ApiProperties props) {
		this.maxAttempts = props.login().maxAttempts();
		this.lock = Duration.ofMinutes(props.login().lockMinutes());
	}

	boolean isLocked(String document) {
		State s = states.get(document);
		if (s == null || s.lockedUntil() == null) {
			return false;
		}
		if (Instant.now().isAfter(s.lockedUntil())) {
			states.remove(document);
			return false;
		}
		return true;
	}

	void failure(String document) {
		states.merge(document, new State(1, null), (old, one) -> {
			int failures = old.failures() + 1;
			return new State(failures, failures >= maxAttempts ? Instant.now().plus(lock) : null);
		});
	}

	void success(String document) {
		states.remove(document);
	}
}
