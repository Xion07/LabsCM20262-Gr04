package co.edu.udea.motoscaribe.api.auth;

import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;

import co.edu.udea.motoscaribe.api.auth.AuthController.TokenResponse;
import co.edu.udea.motoscaribe.api.auth.ChallengeStore.Purpose;
import co.edu.udea.motoscaribe.api.auth.PasskeyStore.Passkey;
import co.edu.udea.motoscaribe.api.config.ApiProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Passkeys (FIDO2 / WebAuthn). El telefono guarda una llave privada que nunca sale de el y que solo se
 * usa tras el desbloqueo del usuario (huella, rostro o PIN); el servidor solo conoce la llave publica.
 * Registro: con sesion iniciada (clave), crea la llave. Ingreso: el telefono firma un reto del servidor,
 * sin escribir cedula ni clave. Las respuestas se validan con webauthn4j (origen, dominio, reto, firma,
 * contador y verificacion del usuario).
 */
@RestController
@RequestMapping("/api/v1")
class PasskeyController {

	private static final Logger log = LoggerFactory.getLogger(PasskeyController.class);
	private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
	private static final long TIMEOUT_MS = 60_000;
	private static final int MAX_PER_USER = 5;

	private final WebAuthnManager manager = WebAuthnManager.createNonStrictWebAuthnManager();
	private final ChallengeStore challenges;
	private final PasskeyStore store;
	private final UserDirectory users;
	private final TokenService tokens;
	private final RefreshTokenStore refreshTokens;
	private final String rpId;
	private final String rpName;
	private final Set<Origin> origins;

	PasskeyController(ApiProperties props, ChallengeStore challenges, PasskeyStore store, UserDirectory users,
			TokenService tokens, RefreshTokenStore refreshTokens) {
		this.challenges = challenges;
		this.store = store;
		this.users = users;
		this.tokens = tokens;
		this.refreshTokens = refreshTokens;
		ApiProperties.Passkeys cfg = props.passkeys();
		this.rpId = cfg.rpId();
		this.rpName = cfg.rpName();
		this.origins = Arrays.stream(cfg.origins().split(","))
				.map(String::trim).filter(s -> !s.isEmpty()).map(Origin::new).collect(Collectors.toSet());
	}

	// ---- Registro (requiere sesion)

	/** Opciones para crear el passkey: se pasan tal cual a Credential Manager (requestJson). */
	@PostMapping("/passkeys/register/options")
	Map<String, Object> registerOptions(@AuthenticationPrincipal Jwt jwt) {
		String document = jwt.getSubject();
		UserDirectory.UserAccount account = users.find(document)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		List<Passkey> existing = store.of(document);
		if (existing.size() >= MAX_PER_USER) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes el máximo de passkeys");
		}
		Map<String, Object> options = new LinkedHashMap<>();
		options.put("challenge", B64.encodeToString(challenges.issue(Purpose.REGISTER, document)));
		options.put("rp", Map.of("id", rpId, "name", rpName));
		options.put("user", Map.of(
				"id", B64.encodeToString(store.userHandle(document)),
				"name", document,
				"displayName", account.holderName()));
		options.put("pubKeyCredParams", List.of(
				Map.of("type", "public-key", "alg", -7), // ES256
				Map.of("type", "public-key", "alg", -257))); // RS256
		options.put("timeout", TIMEOUT_MS);
		options.put("attestation", "none");
		// Evita crear un segundo passkey en el mismo dispositivo/gestor.
		options.put("excludeCredentials", existing.stream()
				.map(p -> Map.of("type", "public-key", "id", B64.encodeToString(p.credentialId))).toList());
		options.put("authenticatorSelection", Map.of("residentKey", "required", "requireResidentKey", true,
				"userVerification", "required"));
		return options;
	}

	/** Recibe la respuesta de Credential Manager (JSON estandar de WebAuthn) y guarda la llave publica. */
	@PostMapping("/passkeys/register/verify")
	ResponseEntity<Void> registerVerify(@AuthenticationPrincipal Jwt jwt, @RequestBody String credentialJson) {
		String document = jwt.getSubject();
		try {
			RegistrationData parsed = manager.parseRegistrationResponseJSON(credentialJson);
			byte[] challenge = parsed.getCollectedClientData().getChallenge().getValue();
			String owner = challenges.consume(challenge, Purpose.REGISTER)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reto inválido o vencido"));
			if (!owner.equals(document)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reto inválido o vencido");
			}
			ServerProperty server = new ServerProperty(origins, rpId, new DefaultChallenge(challenge));
			manager.verify(parsed, new RegistrationParameters(server, true, true));

			CredentialRecordImpl record = new CredentialRecordImpl(parsed.getAttestationObject(),
					parsed.getCollectedClientData(), parsed.getClientExtensions(), parsed.getTransports());
			var data = parsed.getAttestationObject().getAuthenticatorData();
			byte[] credentialId = data.getAttestedCredentialData().getCredentialId();
			store.add(new Passkey(document, credentialId, data.getAttestedCredentialData(), data.getSignCount(),
					record.isBackupEligible() != null && record.isBackupEligible(),
					record.isBackedUp() != null && record.isBackedUp(), Instant.now()));
			return ResponseEntity.noContent().build();
		}
		catch (ResponseStatusException e) {
			throw e;
		}
		catch (RuntimeException e) {
			log.info("Registro de passkey rechazado: {}", e.toString());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo verificar el passkey");
		}
	}

	record PasskeyView(String id, Instant createdAt, boolean synced) {
	}

	@GetMapping("/passkeys")
	List<PasskeyView> list(@AuthenticationPrincipal Jwt jwt) {
		return store.of(jwt.getSubject()).stream()
				.map(p -> new PasskeyView(B64.encodeToString(p.credentialId).substring(0, 8), p.createdAt, p.backedUp))
				.toList();
	}

	/** Quita todos los passkeys del usuario (para volver a registrar o si cambia de telefono). */
	@DeleteMapping("/passkeys")
	ResponseEntity<Void> removeAll(@AuthenticationPrincipal Jwt jwt) {
		store.removeAll(jwt.getSubject());
		return ResponseEntity.noContent().build();
	}

	// ---- Ingreso (publico: se autentica con la firma)

	/** Reto para ingresar. Sin lista de credenciales: el telefono muestra los passkeys de este sitio. */
	@PostMapping("/auth/passkey/options")
	Map<String, Object> loginOptions() {
		Map<String, Object> options = new LinkedHashMap<>();
		options.put("challenge", B64.encodeToString(challenges.issue(Purpose.LOGIN, "")));
		options.put("rpId", rpId);
		options.put("timeout", TIMEOUT_MS);
		options.put("userVerification", "required");
		options.put("allowCredentials", List.of());
		return options;
	}

	@PostMapping("/auth/passkey/verify")
	TokenResponse loginVerify(@RequestBody String credentialJson) {
		try {
			AuthenticationData parsed = manager.parseAuthenticationResponseJSON(credentialJson);
			byte[] challenge = parsed.getCollectedClientData().getChallenge().getValue();
			challenges.consume(challenge, Purpose.LOGIN)
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Reto inválido o vencido"));
			Passkey passkey = store.find(parsed.getCredentialId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Passkey no registrado"));
			// El userHandle que firma el telefono debe ser el de la cuenta dueña de la credencial.
			byte[] handle = parsed.getUserHandle();
			if (handle != null && !Arrays.equals(handle, store.userHandle(passkey.document))) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Passkey no válido");
			}
			ServerProperty server = new ServerProperty(origins, rpId, new DefaultChallenge(challenge));
			manager.verify(parsed, new AuthenticationParameters(server, passkey.toRecord(), null, true, true));
			store.update(passkey, parsed.getAuthenticatorData().getSignCount(),
					parsed.getAuthenticatorData().isFlagBS());
			return new TokenResponse(tokens.issue(passkey.document), refreshTokens.issue(passkey.document), "Bearer",
					tokens.ttlSeconds());
		}
		catch (ResponseStatusException e) {
			throw e;
		}
		catch (RuntimeException e) {
			log.info("Ingreso con passkey rechazado: {}", e.toString());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No se pudo verificar el passkey");
		}
	}
}
