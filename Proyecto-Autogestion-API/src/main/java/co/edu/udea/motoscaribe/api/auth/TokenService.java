package co.edu.udea.motoscaribe.api.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import co.edu.udea.motoscaribe.api.config.ApiProperties;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
class TokenService {

	private final JwtEncoder encoder;
	private final ApiProperties props;

	TokenService(JwtEncoder encoder, ApiProperties props) {
		this.encoder = encoder;
		this.props = props;
	}

	/** Token de vida corta; el sujeto es la cedula del arrendatario. */
	String issue(String document) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("autogestion-api")
				.subject(document)
				.issuedAt(now)
				.expiresAt(now.plus(props.jwt().ttlMinutes(), ChronoUnit.MINUTES))
				.claim("scope", "arrendatario")
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	long ttlSeconds() {
		return props.jwt().ttlMinutes() * 60L;
	}
}
