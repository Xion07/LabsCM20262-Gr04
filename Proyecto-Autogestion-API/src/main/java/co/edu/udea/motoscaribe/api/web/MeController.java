package co.edu.udea.motoscaribe.api.web;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import co.edu.udea.motoscaribe.api.credit.Credit;
import co.edu.udea.motoscaribe.api.credit.CreditData;
import co.edu.udea.motoscaribe.api.upload.AttachmentStore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Todo lo que ve el arrendatario autenticado. La cedula sale siempre del token. */
@RestController
@RequestMapping("/api/v1/me")
class MeController {

	private static final String UUID_RE = "[0-9a-fA-F-]{36}";

	private final CreditData data;
	private final AttachmentStore attachments;

	MeController(CreditData data, AttachmentStore attachments) {
		this.data = data;
		this.attachments = attachments;
	}

	@GetMapping("/contract")
	Credit.Contract contract(@AuthenticationPrincipal Jwt jwt) {
		return data.contract(jwt.getSubject())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No tienes un contrato activo"));
	}

	@GetMapping("/payments")
	List<Credit.Payment> payments(@AuthenticationPrincipal Jwt jwt) {
		return data.payments(jwt.getSubject());
	}

	@GetMapping("/fines")
	List<Credit.Fine> fines(@AuthenticationPrincipal Jwt jwt) {
		return data.fines(jwt.getSubject());
	}

	@GetMapping("/km-history")
	List<Credit.KmReading> kmHistory(@AuthenticationPrincipal Jwt jwt) {
		return data.kmHistory(jwt.getSubject());
	}

	record ExtensionBody(
			@NotNull LocalDate dueDate,
			LocalDate newDate,
			@NotBlank @Size(min = 10, max = 500, message = "El motivo debe tener entre 10 y 500 caracteres") String excuse) {

		@AssertTrue(message = "newDate debe ser posterior a dueDate")
		boolean isNewDateValid() {
			return dueDate == null || newDate == null || newDate.isAfter(dueDate);
		}
	}

	record ExtensionView(String id, LocalDate dueDate, LocalDate newDate, String excuse, boolean hasAttachment,
			String status, Instant createdAt) {

		static ExtensionView of(Credit.ExtensionRecord r) {
			return new ExtensionView(r.id(), r.dueDate(), r.newDate(), r.excuse(), r.attachmentId() != null,
					r.status(), r.createdAt());
		}
	}

	@GetMapping("/extension-requests")
	List<ExtensionView> extensions(@AuthenticationPrincipal Jwt jwt) {
		return data.listExtensions(jwt.getSubject()).stream().map(ExtensionView::of).toList();
	}

	@PostMapping("/extension-requests")
	@ResponseStatus(HttpStatus.CREATED)
	ExtensionView createExtension(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ExtensionBody body) {
		return ExtensionView.of(data.saveExtension(jwt.getSubject(), body.dueDate(), body.newDate(), body.excuse().trim()));
	}

	@PostMapping(path = "/extension-requests/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ExtensionView attach(@AuthenticationPrincipal Jwt jwt,
			@PathVariable @Pattern(regexp = UUID_RE) String id,
			@RequestPart("file") MultipartFile file) throws IOException {
		// Si la solicitud no es del usuario del token se responde 404 (no se revela que existe).
		data.findExtension(jwt.getSubject(), id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
		String attachmentId = attachments.save(file);
		data.attach(jwt.getSubject(), id, attachmentId);
		return ExtensionView.of(data.findExtension(jwt.getSubject(), id).orElseThrow());
	}
}
