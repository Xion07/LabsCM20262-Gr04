package co.edu.udea.motoscaribe.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracion propia de la API (prefijo {@code api.*}). */
@ConfigurationProperties(prefix = "api")
public record ApiProperties(Jwt jwt, Uploads uploads, Login login, Mock mock, Seed seed) {

	/** @param secret clave HMAC (minimo 32 caracteres), viene de la variable API_JWT_SECRET */
	public record Jwt(String secret, int ttlMinutes) {
	}

	public record Uploads(String dir) {
	}

	public record Login(int maxAttempts, int lockMinutes) {
	}

	/** Datos de demostracion; solo se usan con el perfil {@code mock}. */
	public record Mock(String document, String password, String holder) {
	}

	/** Clave compartida de los clientes ficticios del perfil {@code seed}. */
	public record Seed(String password) {
	}
}
