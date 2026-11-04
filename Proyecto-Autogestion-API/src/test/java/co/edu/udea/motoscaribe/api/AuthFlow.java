package co.edu.udea.motoscaribe.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import com.jayway.jsonpath.JsonPath;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Ayuda para las pruebas: hace el inicio de sesion con PKCE como lo hace la app. */
final class AuthFlow {

	static final String AUTHORIZE = "/api/v1/auth/authorize";
	static final String TOKEN = "/api/v1/auth/token";
	static final String REFRESH = "/api/v1/auth/refresh";

	private AuthFlow() {
	}

	static String newVerifier() {
		byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String challenge(String verifier) throws Exception {
		byte[] hash = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
		return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
	}

	static String authorizeBody(String document, String password, String verifier) throws Exception {
		return "{\"document\":\"" + document + "\",\"password\":\"" + password + "\",\"codeChallenge\":\""
				+ challenge(verifier) + "\",\"codeChallengeMethod\":\"S256\"}";
	}

	static String tokenBody(String code, String verifier) {
		return "{\"code\":\"" + code + "\",\"codeVerifier\":\"" + verifier + "\"}";
	}

	/** Paso 1 con exito: devuelve el codigo. */
	static String authorize(MockMvc mvc, String document, String password, String verifier) throws Exception {
		String res = mvc.perform(post(AUTHORIZE).contentType(MediaType.APPLICATION_JSON)
				.content(authorizeBody(document, password, verifier)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(res, "$.code");
	}

	/** Inicio de sesion completo: devuelve el cuerpo JSON con access y refresh token. */
	static String login(MockMvc mvc, String document, String password) throws Exception {
		String verifier = newVerifier();
		String code = authorize(mvc, document, password, verifier);
		return mvc.perform(post(TOKEN).contentType(MediaType.APPLICATION_JSON).content(tokenBody(code, verifier)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}
}
