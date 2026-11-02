package co.edu.udea.motoscaribe.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Perfil de demostracion: 20 clientes ficticios con la misma clave. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("seed")
class SeedFlowTest {

	@Autowired
	MockMvc mvc;

	private String login(String document) throws Exception {
		String res = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"document\":\"" + document + "\",\"password\":\"demo1234\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return "Bearer " + JsonPath.read(res, "$.accessToken");
	}

	private String contract(String document) throws Exception {
		return mvc.perform(get("/api/v1/me/contract").header("Authorization", login(document)))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
	}

	@Test
	void allTwentyClientsCanSignInAndSeeTheirContract() throws Exception {
		for (int i = 1; i <= 20; i++) {
			String document = String.valueOf(1_000_000_000L + i);
			String body = contract(document);
			org.junit.jupiter.api.Assertions.assertNotNull(JsonPath.read(body, "$.holderName"), document);
		}
	}

	@Test
	void eachClientOnlySeesTheirOwnData() throws Exception {
		String a = contract("1000000001");
		String b = contract("1000000002");
		org.junit.jupiter.api.Assertions.assertNotEquals(
				JsonPath.read(a, "$.plate").toString(), JsonPath.read(b, "$.plate").toString());
		org.junit.jupiter.api.Assertions.assertEquals("Camilo Restrepo", JsonPath.read(a, "$.holderName"));
	}

	@Test
	void showcaseScenariosAreCovered() throws Exception {
		// 2: en mora; 3: vence hoy; 4: vence manana
		LocalDate today = LocalDate.now();
		org.junit.jupiter.api.Assertions.assertTrue(
				LocalDate.parse(JsonPath.read(contract("1000000002"), "$.nextPaymentDate")).isBefore(today));
		org.junit.jupiter.api.Assertions.assertEquals(today.toString(),
				JsonPath.read(contract("1000000003"), "$.nextPaymentDate"));
		org.junit.jupiter.api.Assertions.assertEquals(today.plusDays(1).toString(),
				JsonPath.read(contract("1000000004"), "$.nextPaymentDate"));

		// 5: sin multas; 18: sin abonos (estados vacios de la app)
		mvc.perform(get("/api/v1/me/fines").header("Authorization", login("1000000005")))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
		mvc.perform(get("/api/v1/me/payments").header("Authorization", login("1000000018")))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void listsAreConsistent() throws Exception {
		String auth = login("1000000001");
		mvc.perform(get("/api/v1/me/payments").header("Authorization", auth))
				.andExpect(jsonPath("$", hasSize(4)));
		mvc.perform(get("/api/v1/me/km-history").header("Authorization", auth))
				.andExpect(jsonPath("$", hasSize(4)))
				.andExpect(jsonPath("$[0].km", lessThan(2871)));
	}

	@Test
	void extensionHistoryIsPerUserAndNewestFirst() throws Exception {
		String a = login("1000000009");
		String b = login("1000000010");
		for (String excuse : new String[] { "Primera solicitud de prueba", "Segunda solicitud de prueba" }) {
			mvc.perform(post("/api/v1/me/extension-requests").header("Authorization", a)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"dueDate\":\"" + LocalDate.now() + "\",\"excuse\":\"" + excuse + "\"}"))
					.andExpect(status().isCreated());
		}
		mvc.perform(get("/api/v1/me/extension-requests").header("Authorization", a))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].excuse").value("Segunda solicitud de prueba"));
		mvc.perform(get("/api/v1/me/extension-requests").header("Authorization", b))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void homePageIsPublicButApiStillNeedsToken() throws Exception {
		mvc.perform(get("/")).andExpect(status().isOk());
		mvc.perform(get("/api/v1/me/contract")).andExpect(status().isUnauthorized());
	}

	@Test
	void unknownDocumentIsRejected() throws Exception {
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"document\":\"999999999\",\"password\":\"demo1234\"}"))
				.andExpect(status().isUnauthorized());
	}
}
