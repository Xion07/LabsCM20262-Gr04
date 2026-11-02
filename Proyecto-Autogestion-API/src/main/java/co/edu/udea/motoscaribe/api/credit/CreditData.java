package co.edu.udea.motoscaribe.api.credit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a los datos del arrendatario. Todos los metodos reciben la cedula del TOKEN,
 * nunca una enviada por el cliente. Hoy la implementa {@link MockCreditData}; la real
 * leera vistas de MySQL (solo lectura) y guardara las solicitudes en una tabla propia.
 */
public interface CreditData {

	Optional<Credit.Contract> contract(String document);

	List<Credit.Payment> payments(String document);

	List<Credit.Fine> fines(String document);

	List<Credit.KmReading> kmHistory(String document);

	Credit.ExtensionRecord saveExtension(String document, LocalDate dueDate, LocalDate newDate, String excuse);

	/** Solo devuelve la solicitud si pertenece a {@code document}. */
	Optional<Credit.ExtensionRecord> findExtension(String document, String id);

	/** Solicitudes del dueno, la mas reciente primero. */
	List<Credit.ExtensionRecord> listExtensions(String document);

	void attach(String document, String id, String attachmentId);
}
