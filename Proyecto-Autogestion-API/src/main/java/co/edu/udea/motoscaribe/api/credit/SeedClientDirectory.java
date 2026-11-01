package co.edu.udea.motoscaribe.api.credit;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Lista publica (para el login de la demo) de cedula -> nombre de los clientes ficticios. */
@Component
@Profile("seed")
public class SeedClientDirectory {

	private final Map<String, String> holders;

	SeedClientDirectory(SeedCreditData data) {
		this.holders = data.clients().values().stream()
				.collect(Collectors.toUnmodifiableMap(SeedClients.Client::document, SeedClients.Client::holder));
	}

	public Map<String, String> holders() {
		return holders;
	}
}
