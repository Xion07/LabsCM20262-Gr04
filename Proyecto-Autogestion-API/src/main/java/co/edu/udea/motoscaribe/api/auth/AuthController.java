package co.edu.udea.motoscaribe.api.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

	/** Hash de relleno: igualar el tiempo de respuesta cuando la cedula no existe. */
	private final String dummyHash;

	private final UserDirectory users;
	private final PasswordEncoder encoder;
	private final TokenService tokens;
	private final LoginThrottle throttle;

	AuthController(UserDirectory users, PasswordEncoder encoder, TokenService tokens, LoginThrottle throttle) {
		this.users = users;
		this.encoder = encoder;
		this.tokens = tokens;
		this.throttle = throttle;
		this.dummyHash = encoder.encode("relleno-sin-uso");
	}

	record LoginRequest(
			@NotBlank @Pattern(regexp = "\\d{6,12}", message = "La cédula debe tener entre 6 y 12 números") String document,
			@NotBlank @Size(min = 4, max = 100) String password) {
	}

	record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {
	}

	@PostMapping("/login")
	TokenResponse login(@Valid @RequestBody LoginRequest request) {
		if (throttle.isLocked(request.document())) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Intenta más tarde.");
		}
		var account = users.find(request.document());
		boolean ok = encoder.matches(request.password(), account.map(UserDirectory.UserAccount::passwordHash).orElse(dummyHash))
				&& account.isPresent();
		if (!ok) {
			throttle.failure(request.document());
			// Mismo mensaje para cedula inexistente y clave incorrecta.
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
		}
		throttle.success(request.document());
		return new TokenResponse(tokens.issue(request.document()), "Bearer", tokens.ttlSeconds());
	}
}
