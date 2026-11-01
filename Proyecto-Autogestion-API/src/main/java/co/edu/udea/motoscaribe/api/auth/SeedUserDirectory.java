package co.edu.udea.motoscaribe.api.auth;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import co.edu.udea.motoscaribe.api.config.ApiProperties;
import co.edu.udea.motoscaribe.api.credit.SeedClientDirectory;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cuentas de los 20 clientes ficticios. Todos comparten la clave de demostracion
 * ({@code api.seed.password}), guardada como hash BCrypt. Solo para la demo de la universidad.
 */
@Component
@Profile("seed")
class SeedUserDirectory implements UserDirectory {

	private final Map<String, UserAccount> accounts;

	SeedUserDirectory(ApiProperties props, PasswordEncoder encoder, SeedClientDirectory clients) {
		String hash = encoder.encode(props.seed().password());
		this.accounts = clients.holders().entrySet().stream().collect(Collectors.toUnmodifiableMap(
				Map.Entry::getKey, e -> new UserAccount(e.getKey(), hash, e.getValue())));
	}

	@Override
	public Optional<UserAccount> find(String document) {
		return Optional.ofNullable(accounts.get(document));
	}
}
