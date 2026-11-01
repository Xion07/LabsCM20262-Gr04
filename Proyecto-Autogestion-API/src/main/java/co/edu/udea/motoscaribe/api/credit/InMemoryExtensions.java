package co.edu.udea.motoscaribe.api.credit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Solicitudes de plazo en memoria (se pierden al reiniciar): suficiente para la demo. */
class InMemoryExtensions {

	private final ConcurrentHashMap<String, Credit.ExtensionRecord> byId = new ConcurrentHashMap<>();

	Credit.ExtensionRecord save(String document, LocalDate dueDate, LocalDate newDate, String excuse) {
		var record = new Credit.ExtensionRecord(UUID.randomUUID().toString(), document, dueDate, newDate, excuse,
				null, "PENDING_REVIEW", Instant.now());
		byId.put(record.id(), record);
		return record;
	}

	/** Solo devuelve la solicitud si pertenece a {@code document}. */
	Optional<Credit.ExtensionRecord> find(String document, String id) {
		return Optional.ofNullable(byId.get(id)).filter(r -> r.document().equals(document));
	}

	void attach(String document, String id, String attachmentId) {
		find(document, id).ifPresent(r -> byId.put(id, r.withAttachment(attachmentId)));
	}
}
