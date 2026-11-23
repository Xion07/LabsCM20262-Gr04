package co.edu.udea.motoscaribe.api.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.credential.CredentialRecordImpl;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.NoneAttestationStatement;

import co.edu.udea.motoscaribe.api.config.ApiProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Llaves publicas de los passkeys. De cada credencial solo se guarda la llave PUBLICA (la privada nunca
 * sale del telefono). Si api.passkeys.file esta definido se escribe ahi para sobrevivir a los reinicios;
 * si no, quedan en memoria.
 */
@Component
class PasskeyStore {

	private static final Logger log = LoggerFactory.getLogger(PasskeyStore.class);
	private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder UN_B64 = Base64.getUrlDecoder();

	/** Una credencial registrada: de que cedula es y su estado (contador de firmas, respaldo). */
	static final class Passkey {
		final String document;
		final byte[] credentialId;
		final AttestedCredentialData credentialData;
		final Instant createdAt;
		long counter;
		boolean backupEligible;
		boolean backedUp;

		Passkey(String document, byte[] credentialId, AttestedCredentialData credentialData, long counter,
				boolean backupEligible, boolean backedUp, Instant createdAt) {
			this.document = document;
			this.credentialId = credentialId;
			this.credentialData = credentialData;
			this.counter = counter;
			this.backupEligible = backupEligible;
			this.backedUp = backedUp;
			this.createdAt = createdAt;
		}

		/** Vista para webauthn4j al verificar una firma. */
		CredentialRecordImpl toRecord() {
			return new CredentialRecordImpl(new NoneAttestationStatement(), true, backupEligible, backedUp, counter,
					credentialData, null, null, null, null);
		}
	}

	private final ObjectConverter objects = new ObjectConverter();
	private final AttestedCredentialDataConverter dataConverter = new AttestedCredentialDataConverter(objects);
	private final SecureRandom random = new SecureRandom();
	private final Map<String, Passkey> byCredentialId = new HashMap<>();
	private final Map<String, byte[]> userHandles = new HashMap<>();
	private final Path file;

	PasskeyStore(ApiProperties props) {
		String path = props.passkeys() == null ? null : props.passkeys().file();
		this.file = path == null || path.isBlank() ? null : Path.of(path);
		load();
	}

	/** Identificador opaco y estable del usuario para WebAuthn (no es la cedula). */
	synchronized byte[] userHandle(String document) {
		return userHandles.computeIfAbsent(document, d -> {
			byte[] handle = new byte[32];
			random.nextBytes(handle);
			return handle;
		});
	}

	synchronized void add(Passkey passkey) {
		byCredentialId.put(B64.encodeToString(passkey.credentialId), passkey);
		save();
	}

	synchronized Optional<Passkey> find(byte[] credentialId) {
		return Optional.ofNullable(byCredentialId.get(B64.encodeToString(credentialId)));
	}

	synchronized List<Passkey> of(String document) {
		return byCredentialId.values().stream().filter(p -> p.document.equals(document)).toList();
	}

	/** Guarda el contador y el estado de respaldo tras un inicio de sesion. */
	synchronized void update(Passkey passkey, long counter, boolean backedUp) {
		passkey.counter = counter;
		passkey.backedUp = backedUp;
		save();
	}

	synchronized int removeAll(String document) {
		int before = byCredentialId.size();
		byCredentialId.values().removeIf(p -> p.document.equals(document));
		save();
		return before - byCredentialId.size();
	}

	// ---- archivo: una linea por registro, campos separados por tabulador

	private void save() {
		if (file == null) {
			return;
		}
		List<String> lines = new ArrayList<>();
		userHandles.forEach((doc, handle) -> lines.add("U\t" + doc + "\t" + B64.encodeToString(handle)));
		byCredentialId.values().forEach(p -> lines.add(String.join("\t", "C", p.document,
				B64.encodeToString(p.credentialId), B64.encodeToString(dataConverter.convert(p.credentialData)),
				Long.toString(p.counter), Boolean.toString(p.backupEligible), Boolean.toString(p.backedUp),
				p.createdAt.toString())));
		try {
			if (file.getParent() != null) {
				Files.createDirectories(file.getParent());
			}
			Files.write(file, lines, StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			log.warn("No se pudieron guardar los passkeys en {}: {}", file, e.getMessage());
		}
	}

	private void load() {
		if (file == null || !Files.isRegularFile(file)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				String[] f = line.split("\t");
				if (f[0].equals("U") && f.length == 3) {
					userHandles.put(f[1], UN_B64.decode(f[2]));
				}
				else if (f[0].equals("C") && f.length == 8) {
					byte[] id = UN_B64.decode(f[2]);
					byCredentialId.put(f[2], new Passkey(f[1], id, dataConverter.convert(UN_B64.decode(f[3])),
							Long.parseLong(f[4]), Boolean.parseBoolean(f[5]), Boolean.parseBoolean(f[6]),
							Instant.parse(f[7])));
				}
			}
		}
		catch (IOException | RuntimeException e) {
			log.warn("No se pudieron leer los passkeys de {}: {}", file, e.getMessage());
		}
	}
}
