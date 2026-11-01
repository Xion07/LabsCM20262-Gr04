package co.edu.udea.motoscaribe.api.auth;

import java.util.Optional;

/**
 * Fuente de las cuentas de los arrendatarios. Hoy solo existe la version simulada:
 * la BD de Motos del Caribe no guarda credenciales de arrendatarios, asi que la
 * autenticacion real (clave propia u OTP por WhatsApp) esta por decidir.
 */
public interface UserDirectory {

	Optional<UserAccount> find(String document);

	record UserAccount(String document, String passwordHash, String holderName) {
	}
}
