package co.edu.udea.motoscaribe.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthPkceTest {

	@Autowired
	MockMvc mvc;

	@Test
	void challengeMatchesRfc7636Vector() throws Exception {
		// Apendice B del RFC 7636.
		Assertions.assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
				AuthFlow.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"));
	}

	@Test
	void fullFlowReturnsAccessAndRefreshTokens() throws Exception {
		String res = AuthFlow.login(mvc, "1000000001", "demo1234");
		String access = JsonPath.read(res, "$.accessToken");
		Assertions.assertEquals("Bearer", JsonPath.read(res, "$.tokenType"));
		Assertions.assertEquals(900, (int) JsonPath.read(res, "$.expiresInSeconds"));
		Assertions.assertFalse(((String) JsonPath.read(res, "$.refreshToken")).isEmpty());
		mvc.perform(get("/api/v1/me/contract").header("Authorization", "Bearer " + access))
				.andExpect(status().isOk());
	}

	@Test
	void codeWorksOnlyOnce() throws Exception {
		String verifier = AuthFlow.newVerifier();
		String code = AuthFlow.authorize(mvc, "1000000001", "demo1234", verifier);
		mvc.perform(post(AuthFlow.TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(AuthFlow.tokenBody(code, verifier))).andExpect(status().isOk());
		mvc.perform(post(AuthFlow.TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(AuthFlow.tokenBody(code, verifier))).andExpect(status().isBadRequest());
	}

	@Test
	void interceptedCodeIsUselessWithoutTheVerifier() throws Exception {
		String code = AuthFlow.authorize(mvc, "1000000001", "demo1234", AuthFlow.newVerifier());
		// Un atacante que robo el codigo prueba con otro verifier...
		mvc.perform(post(AuthFlow.TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(AuthFlow.tokenBody(code, AuthFlow.newVerifier()))).andExpect(status().isBadRequest());
	}

	@Test
	void failedExchangeBurnsTheCode() throws Exception {
		String verifier = AuthFlow.newVerifier();
		String code = AuthFlow.authorize(mvc, "1000000001", "demo1234", verifier);
		mvc.perform(post(AuthFlow.TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(AuthFlow.tokenBody(code, AuthFlow.newVerifier()))).andExpect(status().isBadRequest());
		// ...y despues ni siquiera el dueño legitimo puede canjearlo: hay que pedir otro.
		mvc.perform(post(AuthFlow.TOKEN).contentType(MediaType.APPLICATION_JSON)
				.content(AuthFlow.tokenBody(code, verifier))).andExpect(status().isBadRequest());
	}

	@Test
	void onlyS256IsAccepted() throws Exception {
		String body = "{\"document\":\"1000000001\",\"password\":\"demo1234\","
				+ "\"codeChallenge\":\"" + AuthFlow.challenge(AuthFlow.newVerifier()) + "\",\"codeChallengeMethod\":\"plain\"}";
		mvc.perform(post(AuthFlow.AUTHORIZE).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	@Test
	void refreshRotatesTheToken() throws Exception {
		String first = AuthFlow.login(mvc, "1000000001", "demo1234");
		String refresh1 = JsonPath.read(first, "$.refreshToken");
		String res = mvc.perform(post(AuthFlow.REFRESH).contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refresh1 + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", not(emptyString())))
				.andReturn().getResponse().getContentAsString();
		String refresh2 = JsonPath.read(res, "$.refreshToken");
		Assertions.assertNotEquals(refresh1, refresh2);
		// El anterior ya no sirve (rotacion).
		mvc.perform(post(AuthFlow.REFRESH).contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refresh1 + "\"}")).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesTheRefreshToken() throws Exception {
		String res = AuthFlow.login(mvc, "1000000001", "demo1234");
		String refresh = JsonPath.read(res, "$.refreshToken");
		mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refresh + "\"}")).andExpect(status().isNoContent());
		mvc.perform(post(AuthFlow.REFRESH).contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refresh + "\"}")).andExpect(status().isUnauthorized());
	}

	@Test
	void refreshWorksEvenWithAnExpiredAccessTokenInTheHeader() throws Exception {
		String res = AuthFlow.login(mvc, "1000000001", "demo1234");
		String refresh = JsonPath.read(res, "$.refreshToken");
		// Un Bearer invalido o vencido no debe impedir renovar la sesion.
		mvc.perform(post(AuthFlow.REFRESH).header("Authorization", "Bearer vencido.o.invalido")
				.contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"" + refresh + "\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void unknownRefreshTokenIsRejected() throws Exception {
		mvc.perform(post(AuthFlow.REFRESH).contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"inventado\"}")).andExpect(status().isUnauthorized());
	}

	@Test
	void oldDirectLoginEndpointIsGone() throws Exception {
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"document\":\"1000000001\",\"password\":\"demo1234\"}"))
				.andExpect(status().isUnauthorized());
	}
}
