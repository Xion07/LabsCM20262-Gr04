package co.edu.udea.motoscaribe.api.credit;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.json.JsonParserFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * Carga {@code seed/clients.json}: clientes ficticios con fechas como desfases respecto a hoy.
 * Se genera con {@code tools/generate_seed.py}; el formato admite reemplazarlo por datos reales
 * ANONIMIZADOS (sin nombres, cedulas ni placas verdaderas).
 */
final class SeedClients {

	record Payment(int daysAgo, long amount) {
	}

	record Fine(int monthsAgo, long amount, String reason, int installment) {
	}

	record Km(int daysAgo, int km) {
	}

	record Client(String document, String holder, String plate, long totalValue, long balance, long installment,
			int dueInDays, int remainingMonths, int kmTraveled, List<Payment> payments, List<Fine> fines,
			List<Km> kmHistory) {
	}

	private SeedClients() {
	}

	@SuppressWarnings("unchecked")
	static Map<String, Client> load(String resource) {
		try (InputStream in = new ClassPathResource(resource).getInputStream()) {
			String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			var root = JsonParserFactory.getJsonParser().parseMap(json);
			Map<String, Client> byDocument = new LinkedHashMap<>();
			for (Object o : (List<Object>) root.get("clients")) {
				Client c = parse((Map<String, Object>) o);
				byDocument.put(c.document(), c);
			}
			return Map.copyOf(byDocument);
		}
		catch (IOException e) {
			throw new IllegalStateException("No se pudo leer " + resource, e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Client parse(Map<String, Object> m) {
		return new Client(
				(String) m.get("document"), (String) m.get("holder"), (String) m.get("plate"),
				num(m, "totalValue"), num(m, "balance"), num(m, "installment"),
				(int) num(m, "dueInDays"), (int) num(m, "remainingMonths"), (int) num(m, "kmTraveled"),
				((List<Map<String, Object>>) m.get("payments")).stream()
						.map(p -> new Payment((int) num(p, "daysAgo"), num(p, "amount"))).toList(),
				((List<Map<String, Object>>) m.get("fines")).stream()
						.map(f -> new Fine((int) num(f, "monthsAgo"), num(f, "amount"), (String) f.get("reason"),
								(int) num(f, "installment")))
						.toList(),
				((List<Map<String, Object>>) m.get("kmHistory")).stream()
						.map(k -> new Km((int) num(k, "daysAgo"), (int) num(k, "km"))).toList());
	}

	private static long num(Map<String, Object> m, String key) {
		return ((Number) m.get(key)).longValue();
	}
}
