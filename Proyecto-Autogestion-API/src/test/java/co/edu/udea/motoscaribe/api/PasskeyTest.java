package co.edu.udea.motoscaribe.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import com.webauthn4j.data.AuthenticatorAssertionResponse;
import com.webauthn4j.data.AuthenticatorAttestationResponse;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredential;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.ResidentKeyRequirement;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.test.authenticator.webauthn.NoneAttestationAuthenticator;
import com.webauthn4j.test.authenticator.webauthn.WebAuthnAuthenticatorAdaptor;
import com.webauthn4j.test.client.ClientPlatform;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Passkeys de punta a punta: un autenticador simulado (webauthn4j-test) hace de telefono, con el origen
 * de la app Android, y el servidor verifica el registro y el ingreso como lo hara con Credential Manager.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PasskeyTest {

	private static final String ORIGIN = "android:apk-key-hash:ky2K_KsLhNPee3-qt6lD8NQNmhfInLxa7mLKL7qItWc";
	private static final String RP_ID = "autogestion-api-jc.azurewebsites.net";
	private static final Base64.Decoder UN_B64 = Base64.getUrlDecoder();
	private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();

	@Autowired
	MockMvc mvc;

	private static ClientPlatform phone(String origin) {
		return new ClientPlatform(new Origin(origin),
				new WebAuthnAuthenticatorAdaptor(new NoneAttestationAuthenticator()));
	}

	/** Las pruebas comparten la API: cada una parte sin passkeys de la cedula de prueba. */
	@BeforeEach
	void clean() throws Exception {
		mvc.perform(delete("/api/v1/passkeys").header("Authorization", "Bearer " + accessToken("1000000001")))
				.andExpect(status().isNoContent());
	}

	private String accessToken(String document) throws Exception {
		return JsonPath.read(AuthFlow.login(mvc, document, "demo1234"), "$.accessToken");
	}

	/** Registra un passkey para la cedula con el telefono dado. Devuelve el status de /register/verify. */
	private int register(ClientPlatform phone, String access) throws Exception {
		String opts = mvc.perform(post("/api/v1/passkeys/register/options").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rp.id").value(RP_ID))
				.andExpect(jsonPath("$.authenticatorSelection.userVerification").value("required"))
				.andReturn().getResponse().getContentAsString();
		PublicKeyCredentialCreationOptions creation = new PublicKeyCredentialCreationOptions(
				new PublicKeyCredentialRpEntity(RP_ID, "Motos del Caribe"),
				new PublicKeyCredentialUserEntity(UN_B64.decode((String) JsonPath.read(opts, "$.user.id")),
						JsonPath.read(opts, "$.user.name"), JsonPath.read(opts, "$.user.displayName")),
				new DefaultChallenge(UN_B64.decode((String) JsonPath.read(opts, "$.challenge"))),
				List.of(new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY,
						COSEAlgorithmIdentifier.ES256)),
				60_000L, null,
				new AuthenticatorSelectionCriteria(null, ResidentKeyRequirement.REQUIRED,
						UserVerificationRequirement.REQUIRED),
				null, null);
		PublicKeyCredential<AuthenticatorAttestationResponse, ?> cred = phone.create(creation);
		// Mismo formato (y campos extra) que entrega Credential Manager en Android.
		String json = "{\"id\":\"" + cred.getId() + "\",\"rawId\":\"" + B64.encodeToString(cred.getRawId())
				+ "\",\"type\":\"public-key\",\"authenticatorAttachment\":\"platform\",\"response\":{"
				+ "\"clientDataJSON\":\"" + B64.encodeToString(cred.getResponse().getClientDataJSON())
				+ "\",\"attestationObject\":\"" + B64.encodeToString(cred.getResponse().getAttestationObject())
				+ "\",\"transports\":[\"internal\",\"hybrid\"],\"authenticatorData\":\""
				+ B64.encodeToString(cred.getResponse().getAuthenticatorData()) + "\",\"publicKeyAlgorithm\":-7,"
				+ "\"publicKey\":\"" + B64.encodeToString(cred.getResponse().getPublicKey())
				+ "\"},\"clientExtensionResults\":{\"credProps\":{\"rk\":true}}}";
		return mvc.perform(post("/api/v1/passkeys/register/verify").header("Authorization", "Bearer " + access)
				.contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse().getStatus();
	}

	/** Pide el reto, lo firma el telefono y devuelve el JSON de respuesta tal como lo manda la app. */
	private String signLogin(ClientPlatform phone, String challengeFromServer, byte[] userHandleOverride)
			throws Exception {
		PublicKeyCredentialRequestOptions request = new PublicKeyCredentialRequestOptions(
				new DefaultChallenge(UN_B64.decode(challengeFromServer)), 60_000L, RP_ID, null,
				UserVerificationRequirement.REQUIRED, null);
		PublicKeyCredential<AuthenticatorAssertionResponse, ?> cred = phone.get(request);
		byte[] handle = userHandleOverride != null ? userHandleOverride : cred.getResponse().getUserHandle();
		return "{\"id\":\"" + cred.getId() + "\",\"rawId\":\"" + B64.encodeToString(cred.getRawId())
				+ "\",\"type\":\"public-key\",\"authenticatorAttachment\":\"platform\",\"clientExtensionResults\":{},"
				+ "\"response\":{\"clientDataJSON\":\""
				+ B64.encodeToString(cred.getResponse().getClientDataJSON()) + "\",\"authenticatorData\":\""
				+ B64.encodeToString(cred.getResponse().getAuthenticatorData()) + "\",\"signature\":\""
				+ B64.encodeToString(cred.getResponse().getSignature()) + "\",\"userHandle\":\""
				+ (handle == null ? "" : B64.encodeToString(handle)) + "\"}}";
	}

	private String loginChallenge() throws Exception {
		String opts = mvc.perform(post("/api/v1/auth/passkey/options"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rpId").value(RP_ID))
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(opts, "$.challenge");
	}

	@Test
	void registerThenLoginWithoutPassword() throws Exception {
		ClientPlatform phone = phone(ORIGIN);
		Assertions.assertEquals(204, register(phone, accessToken("1000000001")));

		String res = mvc.perform(post("/api/v1/auth/passkey/verify").contentType(MediaType.APPLICATION_JSON)
				.content(signLogin(phone, loginChallenge(), null)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String access = JsonPath.read(res, "$.accessToken");
		Assertions.assertFalse(((String) JsonPath.read(res, "$.refreshToken")).isEmpty());
		// El token obtenido con el passkey sirve para consultar el contrato de esa cedula.
		mvc.perform(get("/api/v1/me/contract").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk());
		mvc.perform(get("/api/v1/passkeys").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		mvc.perform(delete("/api/v1/passkeys").header("Authorization", "Bearer " + access))
				.andExpect(status().isNoContent());
	}

	@Test
	void challengeIsSingleUse() throws Exception {
		ClientPlatform phone = phone(ORIGIN);
		Assertions.assertEquals(204, register(phone, accessToken("1000000001")));
		String body = signLogin(phone, loginChallenge(), null);
		mvc.perform(post("/api/v1/auth/passkey/verify").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
		// Repetir la misma respuesta (captura del trafico) ya no sirve.
		mvc.perform(post("/api/v1/auth/passkey/verify").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginWithChallengeTheServerNeverIssuedIsRejected() throws Exception {
		ClientPlatform phone = phone(ORIGIN);
		Assertions.assertEquals(204, register(phone, accessToken("1000000001")));
		String forged = B64.encodeToString(new byte[32]);
		mvc.perform(post("/api/v1/auth/passkey/verify").contentType(MediaType.APPLICATION_JSON)
				.content(signLogin(phone, forged, null)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unknownPasskeyIsRejected() throws Exception {
		// Un telefono con una llave que nunca se registro en el servidor.
		ClientPlatform stranger = phone(ORIGIN);
		Assertions.assertEquals(204, register(stranger, accessToken("1000000001")));
		mvc.perform(delete("/api/v1/passkeys").header("Authorization", "Bearer " + accessToken("1000000001")))
				.andExpect(status().isNoContent());
		mvc.perform(post("/api/v1/auth/passkey/verify").contentType(MediaType.APPLICATION_JSON)
				.content(signLogin(stranger, loginChallenge(), null)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void registrationFromAnotherAppOriginIsRejected() throws Exception {
		// Una app con otra firma (otro hash de certificado) no puede registrar passkeys.
		ClientPlatform other = phone("android:apk-key-hash:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		Assertions.assertEquals(400, register(other, accessToken("1000000001")));
	}

	@Test
	void loginFromAnotherAppOriginIsRejected() throws Exception {
		// Misma llave, pero la firma llega desde otro origen: se rechaza (el origen es parte de lo firmado).
		WebAuthnAuthenticatorAdaptor device = new WebAuthnAuthenticatorAdaptor(new NoneAttestationAuthenticator());
		Assertions.assertEquals(204, register(new ClientPlatform(new Origin(ORIGIN), device), accessToken("1000000001")));
		ClientPlatform phishing = new ClientPlatform(new Origin("https://sitio-falso.example"), device);
		mvc.perform(post("/api/v1/auth/passkey/verify").contentType(MediaType.APPLICATION_JSON)
				.content(signLogin(phishing, loginChallenge(), null)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void passkeyEndpointsNeedSessionToRegister() throws Exception {
		mvc.perform(post("/api/v1/passkeys/register/options")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/passkeys")).andExpect(status().isUnauthorized());
	}

	@Test
	void assetLinksArePublic() throws Exception {
		mvc.perform(get("/.well-known/assetlinks.json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].target.package_name").value("co.edu.udea.motoscaribe.autogestion"))
				.andExpect(jsonPath("$[0].target.sha256_cert_fingerprints[0]").exists());
	}
}
