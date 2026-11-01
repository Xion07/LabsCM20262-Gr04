package co.edu.udea.motoscaribe.api.credit;

import java.time.Instant;
import java.time.LocalDate;

/** Modelos que la app movil consume. Montos en pesos COP. */
public final class Credit {

	private Credit() {
	}

	public record Contract(
			String holderName,
			String plate,
			long totalValue,
			long balance,
			LocalDate nextPaymentDate,
			long nextPaymentAmount,
			int remainingMonths,
			/** Km de la moto desde el inicio del contrato (suma de hist_distancia). */
			int kmTraveled,
			/** Aun no existe en la BD: es una propuesta del proyecto, siempre null por ahora. */
			Integer lastOilChangeKm) {
	}

	public record Payment(LocalDate date, long amount) {
	}

	/** reason es un codigo (p. ej. LATE_PAYMENT); la app lo traduce al idioma del usuario. */
	public record Fine(LocalDate date, long amount, String reason, int installment) {
	}

	public record KmReading(LocalDate date, int km) {
	}

	/** Solicitud guardada; {@code document} es el dueno y nunca se expone en las respuestas. */
	public record ExtensionRecord(
			String id,
			String document,
			LocalDate dueDate,
			LocalDate newDate,
			String excuse,
			String attachmentId,
			String status,
			Instant createdAt) {

		public ExtensionRecord withAttachment(String attachmentId) {
			return new ExtensionRecord(id, document, dueDate, newDate, excuse, attachmentId, status, createdAt);
		}
	}
}
