package co.edu.udea.motoscaribe.api.credit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import co.edu.udea.motoscaribe.api.config.ApiProperties;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Datos de demostracion para el unico usuario simulado. Las solicitudes viven en memoria. */
@Component
@Profile("mock")
class MockCreditData implements CreditData {

	private final String demoDocument;
	private final String holder;
	private final ConcurrentHashMap<String, Credit.ExtensionRecord> extensions = new ConcurrentHashMap<>();

	MockCreditData(ApiProperties props) {
		this.demoDocument = props.mock().document();
		this.holder = props.mock().holder();
	}

	private boolean isDemo(String document) {
		return demoDocument.equals(document);
	}

	@Override
	public Optional<Credit.Contract> contract(String document) {
		if (!isDemo(document)) {
			return Optional.empty();
		}
		return Optional.of(new Credit.Contract(holder, "ABC12D", 6_800_000, 2_450_000,
				LocalDate.now().plusDays(5), 350_000, 14, 2_870, null));
	}

	@Override
	public List<Credit.Payment> payments(String document) {
		if (!isDemo(document)) {
			return List.of();
		}
		LocalDate today = LocalDate.now();
		return List.of(
				new Credit.Payment(today.minusDays(7), 350_000),
				new Credit.Payment(today.minusDays(14), 350_000),
				new Credit.Payment(today.minusDays(21), 350_000),
				new Credit.Payment(today.minusDays(28), 350_000));
	}

	@Override
	public List<Credit.Fine> fines(String document) {
		if (!isDemo(document)) {
			return List.of();
		}
		LocalDate today = LocalDate.now();
		return List.of(
				new Credit.Fine(today.minusMonths(2), 25_000, "LATE_PAYMENT", 12),
				new Credit.Fine(today.minusMonths(5), 25_000, "LATE_PAYMENT", 5));
	}

	@Override
	public List<Credit.KmReading> kmHistory(String document) {
		if (!isDemo(document)) {
			return List.of();
		}
		LocalDate today = LocalDate.now();
		return List.of(
				new Credit.KmReading(today.minusDays(30), 2_050),
				new Credit.KmReading(today.minusDays(20), 2_320),
				new Credit.KmReading(today.minusDays(10), 2_610),
				new Credit.KmReading(today, 2_870));
	}

	@Override
	public Credit.ExtensionRecord saveExtension(String document, LocalDate dueDate, LocalDate newDate, String excuse) {
		var record = new Credit.ExtensionRecord(UUID.randomUUID().toString(), document, dueDate, newDate, excuse,
				null, "PENDING_REVIEW", Instant.now());
		extensions.put(record.id(), record);
		return record;
	}

	@Override
	public Optional<Credit.ExtensionRecord> findExtension(String document, String id) {
		return Optional.ofNullable(extensions.get(id)).filter(r -> r.document().equals(document));
	}

	@Override
	public void attach(String document, String id, String attachmentId) {
		findExtension(document, id).ifPresent(r -> extensions.put(id, r.withAttachment(attachmentId)));
	}
}
