package co.edu.udea.motoscaribe.api.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Inicio de sesion en dos pasos con PKCE (RFC 7636, S256):
 * 1) /authorize: cedula + clave + code_challenge -> codigo de un solo uso (60 s);
 * 2) /token: codigo + code_verifier -> access token (JWT 15 min) y refresh token (7 dias, rotativo).
 * Quien intercepte el codigo no puede canjearlo sin el verifier, que nunca sale de la app.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

	/** Hash de relleno: igualar el tiempo de respuesta cuando la cedula no existe. */
	private final String dummyHash;

	private final UserDirectory users;
	private final PasswordEncoder encoder;
	private final TokenService tokens;
	private final LoginThrottle throttle;
	private final AuthCodeStore codes;
	private final RefreshTokenStore refreshTokens;

	AuthController(UserDirectory users, PasswordEncoder encoder, TokenService tokens, LoginThrottle throttle,
			AuthCodeStore codes, RefreshTokenStore refreshTokens) {
		this.users = users;
		this.encoder = encoder;
		this.tokens = tokens;
		this.throttle = throttle;
		this.codes = codes;
		this.refreshTokens = refreshTokens;
		this.dummyHash = encoder.encode("relleno-sin-uso");
	}

	record AuthorizeRequest(
			@NotBlank @Pattern(regexp = "\\d{6,12}", message = "La cédula debe tener entre 6 y 12 números") String document,
			@NotBlank @Size(min = 4, max = 100) String password,
			@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "code_challenge inválido") String codeChallenge,
			@NotBlank @Pattern(regexp = "S256", message = "Solo se admite S256") String codeChallengeMethod) {
	}

	record AuthorizeResponse(String code, long expiresInSeconds) {
	}

	record TokenRequest(
			@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "código inválido") String code,
			@NotBlank @Pattern(regexp = "[A-Za-z0-9._~-]{43,128}", message = "code_verifier inválido") String codeVerifier) {
	}

	record RefreshRequest(@NotBlank @Size(max = 200) String refreshToken) {
	}

	record LogoutRequest(@NotBlank @Size(max = 200) String refreshToken) {
	}

	record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
	}

	@PostMapping("/authorize")
	AuthorizeResponse authorize(@Valid @RequestBody AuthorizeRequest request) {
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
		String code = codes.issue(request.document(), request.codeChallenge())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Intenta de nuevo"));
		return new AuthorizeResponse(code, AuthCodeStore.TTL.toSeconds());
	}

	@PostMapping("/token")
	TokenResponse token(@Valid @RequestBody TokenRequest request) {
		String document = codes.redeem(request.code(), request.codeVerifier())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido o vencido"));
		return issue(document);
	}

	@PostMapping("/refresh")
	TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		String document = refreshTokens.consume(request.refreshToken())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión vencida"));
		return issue(document);
	}

	/** Cierra la sesion en el servidor: el refresh token deja de servir. Siempre responde 204. */
	@PostMapping("/logout")
	ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
		refreshTokens.revoke(request.refreshToken());
		return ResponseEntity.noContent().build();
	}

	private TokenResponse issue(String document) {
		return new TokenResponse(tokens.issue(document), refreshTokens.issue(document), "Bearer", tokens.ttlSeconds());
	}
}
