package co.edu.udea.motoscaribe.api.upload;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import co.edu.udea.motoscaribe.api.config.ApiProperties;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Guarda los soportes de las excusas. Solo acepta JPEG o PNG reales (por los primeros bytes,
 * no por el nombre ni el Content-Type), limita el tamano en pixeles y vuelve a codificar la imagen,
 * lo que elimina los metadatos EXIF (que pueden traer la ubicacion GPS). Se guardan en una
 * carpeta privada: no hay URL publica.
 */
@Component
public class AttachmentStore {

	private static final int MAX_PIXELS_SIDE = 12_000;

	private final Path dir;

	AttachmentStore(ApiProperties props) {
		this.dir = Path.of(props.uploads().dir()).toAbsolutePath().normalize();
	}

	/** @return identificador del archivo guardado (UUID con extension) */
	public String save(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");
		}
		byte[] bytes = file.getBytes();
		String format = detectFormat(bytes);

		BufferedImage image = decode(bytes);
		BufferedImage clean = "jpg".equals(format) ? toRgb(image) : image;

		String id = UUID.randomUUID() + "." + format;
		Files.createDirectories(dir);
		if (!ImageIO.write(clean, format, dir.resolve(id).toFile())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo procesar la imagen");
		}
		return id;
	}

	private static String detectFormat(byte[] b) {
		if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
			return "jpg";
		}
		if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
			return "png";
		}
		throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Solo se aceptan imágenes JPEG o PNG");
	}

	private static BufferedImage decode(byte[] bytes) throws IOException {
		try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
			if (!readers.hasNext()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no es válida");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(in);
				// Se revisan las dimensiones antes de decodificar (evita imagenes "bomba").
				if (reader.getWidth(0) > MAX_PIXELS_SIDE || reader.getHeight(0) > MAX_PIXELS_SIDE) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen es demasiado grande");
				}
				return reader.read(0);
			} catch (IOException | RuntimeException e) {
				if (e instanceof ResponseStatusException rse) {
					throw rse;
				}
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no es válida");
			} finally {
				reader.dispose();
			}
		}
	}

	/** JPEG no admite transparencia: se pinta sobre blanco. */
	private static BufferedImage toRgb(BufferedImage src) {
		BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
		var g = rgb.createGraphics();
		g.drawImage(src, 0, 0, java.awt.Color.WHITE, null);
		g.dispose();
		return rgb;
	}
}
