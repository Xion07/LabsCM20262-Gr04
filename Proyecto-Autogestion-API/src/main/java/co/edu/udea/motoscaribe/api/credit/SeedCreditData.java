package co.edu.udea.motoscaribe.api.credit;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Datos de demostracion: 20 clientes ficticios, con fechas calculadas respecto a hoy. */
@Component
@Profile("seed")
class SeedCreditData implements CreditData {

	private final Map<String, SeedClients.Client> clients = SeedClients.load("seed/clients.json");
	private final InMemoryExtensions extensions = new InMemoryExtensions();

	/** Cedulas y nombres de los clientes de demostracion (para el login). */
	Map<String, SeedClients.Client> clients() {
		return clients;
	}

	@Override
	public Optional<Credit.Contract> contract(String document) {
		return Optional.ofNullable(clients.get(document)).map(c -> new Credit.Contract(
				c.holder(), c.plate(), c.totalValue(), c.balance(), LocalDate.now().plusDays(c.dueInDays()),
				c.installment(), c.remainingMonths(), c.kmTraveled(), null));
	}

	@Override
	public List<Credit.Payment> payments(String document) {
		LocalDate today = LocalDate.now();
		return Optional.ofNullable(clients.get(document)).map(c -> c.payments().stream()
				.map(p -> new Credit.Payment(today.minusDays(p.daysAgo()), p.amount())).toList())
				.orElse(List.of());
	}

	@Override
	public List<Credit.Fine> fines(String document) {
		LocalDate today = LocalDate.now();
		return Optional.ofNullable(clients.get(document)).map(c -> c.fines().stream()
				.map(f -> new Credit.Fine(today.minusMonths(f.monthsAgo()), f.amount(), f.reason(), f.installment()))
				.toList())
				.orElse(List.of());
	}

	@Override
	public List<Credit.KmReading> kmHistory(String document) {
		LocalDate today = LocalDate.now();
		return Optional.ofNullable(clients.get(document)).map(c -> c.kmHistory().stream()
				.map(k -> new Credit.KmReading(today.minusDays(k.daysAgo()), k.km())).toList())
				.orElse(List.of());
	}

	@Override
	public Credit.ExtensionRecord saveExtension(String document, LocalDate dueDate, LocalDate newDate, String excuse) {
		return extensions.save(document, dueDate, newDate, excuse);
	}

	@Override
	public Optional<Credit.ExtensionRecord> findExtension(String document, String id) {
		return extensions.find(document, id);
	}

	@Override
	public void attach(String document, String id, String attachmentId) {
		extensions.attach(document, id, attachmentId);
	}
}
