package co.edu.udea.motoscaribe.api.auth;

import java.util.Optional;

import co.edu.udea.motoscaribe.api.config.ApiProperties;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Un unico usuario de demostracion, con la clave guardada como hash BCrypt. */
@Component
@Profile("mock")
class MockUserDirectory implements UserDirectory {

	private final UserAccount demo;

	MockUserDirectory(ApiProperties props, PasswordEncoder encoder) {
		ApiProperties.Mock m = props.mock();
		this.demo = new UserAccount(m.document(), encoder.encode(m.password()), m.holder());
	}

	@Override
	public Optional<UserAccount> find(String document) {
		return demo.document().equals(document) ? Optional.of(demo) : Optional.empty();
	}
}
