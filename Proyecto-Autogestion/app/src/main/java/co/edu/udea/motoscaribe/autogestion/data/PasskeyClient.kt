package co.edu.udea.motoscaribe.autogestion.data

import android.app.Activity
import android.content.Context
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException

/** Por que fallo una operacion con passkeys. */
enum class PasskeyFailure {
    /** La persona cerro el dialogo del sistema. No es un error. */
    CANCELLED,
    /** No hay ningun passkey de esta app en el telefono. */
    NONE,
    /** Otro fallo (sin Google Play Services, sin bloqueo de pantalla, dominio no verificado...). */
    FAILED,
}

class PasskeyException(val kind: PasskeyFailure, cause: Throwable? = null) : Exception(kind.name, cause)

/**
 * Passkeys (FIDO2 / WebAuthn) con Credential Manager de Android. La llave privada la crea y guarda el
 * sistema en el telefono (protegida por huella, rostro o PIN) y nunca llega a la app ni al servidor:
 * aqui solo se pasa al sistema el JSON de opciones que manda la API y se devuelve su respuesta firmada.
 */
class PasskeyClient(context: Context) {

    private val manager = CredentialManager.create(context.applicationContext)

    /** Crea un passkey a partir de las opciones de la API y devuelve la respuesta JSON de WebAuthn. */
    suspend fun create(activity: Activity, optionsJson: String): String = try {
        val response = manager.createCredential(activity, CreatePublicKeyCredentialRequest(optionsJson))
        (response as CreatePublicKeyCredentialResponse).registrationResponseJson
    } catch (e: CreateCredentialCancellationException) {
        throw PasskeyException(PasskeyFailure.CANCELLED, e)
    } catch (e: CreateCredentialException) {
        throw PasskeyException(PasskeyFailure.FAILED, e)
    }

    /** Firma el reto de la API con un passkey elegido por la persona y devuelve la respuesta JSON. */
    suspend fun get(activity: Activity, optionsJson: String): String = try {
        val request = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(optionsJson)))
        val credential = manager.getCredential(activity, request).credential
        (credential as? PublicKeyCredential)?.authenticationResponseJson
            ?: throw PasskeyException(PasskeyFailure.FAILED)
    } catch (e: GetCredentialCancellationException) {
        throw PasskeyException(PasskeyFailure.CANCELLED, e)
    } catch (e: NoCredentialException) {
        throw PasskeyException(PasskeyFailure.NONE, e)
    } catch (e: GetCredentialException) {
        throw PasskeyException(PasskeyFailure.FAILED, e)
    }
}
