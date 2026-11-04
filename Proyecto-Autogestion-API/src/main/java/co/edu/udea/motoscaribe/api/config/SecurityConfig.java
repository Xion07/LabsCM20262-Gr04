package co.edu.udea.motoscaribe.api.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API sin sesion: todo exige un JWT salvo el login. Cada consulta se resuelve por el
 * sujeto del token, nunca por una cedula enviada por el cliente.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {

	private static final int MIN_SECRET_BYTES = 32;

	@Bean
	SecretKey jwtKey(ApiProperties props) {
		String secret = props.jwt().secret();
		byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"API_JWT_SECRET debe definirse con al menos " + MIN_SECRET_BYTES + " caracteres");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtKey) {
		return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * En las rutas de autenticacion el encabezado Authorization se ignora: sin esto, un access token
	 * vencido hace rechazar con 401 hasta el refresh, que es justo lo que se usa para renovarlo.
	 */
	private static BearerTokenResolver ignoreTokenOnAuthRoutes() {
		DefaultBearerTokenResolver standard = new DefaultBearerTokenResolver();
		return request -> request.getRequestURI().startsWith("/api/v1/auth/") ? null : standard.resolve(request);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable()) // API sin cookies: el token va en el encabezado
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(a -> a
						.requestMatchers(HttpMethod.GET, "/").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/auth/authorize", "/api/v1/auth/token",
								"/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(o -> o.bearerTokenResolver(ignoreTokenOnAuthRoutes()).jwt(Customizer.withDefaults()));
		return http.build();
	}
}
