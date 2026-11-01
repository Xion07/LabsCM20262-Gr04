package co.edu.udea.motoscaribe.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import javax.imageio.ImageIO;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowTest {

	private static final String LOGIN = "/api/v1/auth/login";

	@Autowired
	MockMvc mvc;

	private String login(String document, String password) throws Exception {
		String body = "{\"document\":\"" + document + "\",\"password\":\"" + password + "\"}";
		String res = mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(res, "$.accessToken");
	}

	private String token() throws Exception {
		return login("1000000001", "demo1234");
	}

	private static byte[] png() throws Exception {
		BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "png", out);
		return out.toByteArray();
	}

	@Test
	void loginReturnsTokenAndShortTtl() throws Exception {
		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content("{\"document\":\"1000000001\",\"password\":\"demo1234\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInSeconds").value(900));
	}

	@Test
	void wrongPasswordAndUnknownDocumentGiveTheSameError() throws Exception {
		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content("{\"document\":\"1000000001\",\"password\":\"incorrecta\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Credenciales inválidas"));
		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content("{\"document\":\"999999999\",\"password\":\"demo1234\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Credenciales inválidas"));
	}

	@Test
	void loginLocksAfterTooManyFailures() throws Exception {
		String body = "{\"document\":\"555555555\",\"password\":\"mala\"}";
		for (int i = 0; i < 5; i++) {
			mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isUnauthorized());
		}
		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isTooManyRequests());
	}

	@Test
	void loginValidatesInput() throws Exception {
		mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content("{\"document\":\"abc\",\"password\":\"x\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void dataRequiresToken() throws Exception {
		mvc.perform(get("/api/v1/me/contract")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/me/contract").header("Authorization", "Bearer basura"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void contractComesFromTheTokenUser() throws Exception {
		mvc.perform(get("/api/v1/me/contract").header("Authorization", "Bearer " + token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.holderName").value("Camilo"))
				.andExpect(jsonPath("$.kmTraveled").value(2870))
				.andExpect(jsonPath("$.lastOilChangeKm").doesNotExist());
	}

	@Test
	void listsAreAvailable() throws Exception {
		String auth = "Bearer " + token();
		mvc.perform(get("/api/v1/me/payments").header("Authorization", auth))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
		mvc.perform(get("/api/v1/me/fines").header("Authorization", auth))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].reason").value("LATE_PAYMENT"));
		mvc.perform(get("/api/v1/me/km-history").header("Authorization", auth))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(4)));
	}

	@Test
	void extensionRequestIsValidated() throws Exception {
		String today = LocalDate.now().toString();
		mvc.perform(post("/api/v1/me/extension-requests").header("Authorization", "Bearer " + token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"dueDate\":\"" + today + "\",\"excuse\":\"corto\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fields.excuse").exists());
		mvc.perform(post("/api/v1/me/extension-requests").header("Authorization", "Bearer " + token())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"dueDate\":\"" + today + "\",\"newDate\":\"" + LocalDate.now().minusDays(1)
						+ "\",\"excuse\":\"Se me dañó la moto esta semana\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void extensionRequestWithImageAttachment() throws Exception {
		String auth = "Bearer " + token();
		String res = mvc.perform(post("/api/v1/me/extension-requests").header("Authorization", auth)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"dueDate\":\"" + LocalDate.now() + "\",\"newDate\":\"" + LocalDate.now().plusDays(3)
						+ "\",\"excuse\":\"Se me dañó la moto esta semana\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
				.andExpect(jsonPath("$.hasAttachment").value(false))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(res, "$.id");

		mvc.perform(multipart("/api/v1/me/extension-requests/" + id + "/attachment")
				.file(new MockMultipartFile("file", "soporte.png", "image/png", png()))
				.header("Authorization", auth))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasAttachment").value(true));
	}

	@Test
	void attachmentThatIsNotAnImageIsRejected() throws Exception {
		String auth = "Bearer " + token();
		String res = mvc.perform(post("/api/v1/me/extension-requests").header("Authorization", auth)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"dueDate\":\"" + LocalDate.now() + "\",\"excuse\":\"Se me dañó la moto esta semana\"}"))
				.andReturn().getResponse().getContentAsString();
		String id = JsonPath.read(res, "$.id");

		// Texto con nombre y tipo de imagen: se rechaza por el contenido real.
		mvc.perform(multipart("/api/v1/me/extension-requests/" + id + "/attachment")
				.file(new MockMultipartFile("file", "soporte.png", "image/png", "no soy una imagen".getBytes()))
				.header("Authorization", auth))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.error", containsString("JPEG o PNG")));
	}

	@Test
	void anotherUsersRequestIsNotFound() throws Exception {
		String auth = "Bearer " + token();
		mvc.perform(multipart("/api/v1/me/extension-requests/00000000-0000-0000-0000-000000000000/attachment")
				.file(new MockMultipartFile("file", "a.png", "image/png", png()))
				.header("Authorization", auth))
				.andExpect(status().isNotFound());
	}
}
