package co.edu.udea.motoscaribe.autogestion.data

import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/** Traduce las respuestas JSON de la API a los modelos de la app. Funciones puras (con pruebas). */
object JsonMapping {

    fun contract(json: String): Contract {
        val o = JSONObject(json)
        return Contract(
            holderName = o.getString("holderName"),
            plate = o.getString("plate"),
            totalValue = o.getLong("totalValue"),
            balance = o.getLong("balance"),
            nextPaymentDate = LocalDate.parse(o.getString("nextPaymentDate")),
            nextPaymentAmount = o.getLong("nextPaymentAmount"),
            remainingMonths = o.getInt("remainingMonths"),
            kmTraveled = o.getInt("kmTraveled"),
            lastOilChangeKm = if (o.isNull("lastOilChangeKm")) null else o.getInt("lastOilChangeKm"),
        )
    }

    fun payments(json: String): List<Payment> = list(json) {
        Payment(LocalDate.parse(it.getString("date")), it.getLong("amount"))
    }

    fun fines(json: String): List<Fine> = list(json) {
        Fine(
            date = LocalDate.parse(it.getString("date")),
            amount = it.getLong("amount"),
            // Un motivo que la app aun no conoce no debe romper la pantalla.
            reason = FineReason.entries.firstOrNull { r -> r.name == it.getString("reason") } ?: FineReason.OTHER,
            installment = it.getInt("installment"),
        )
    }

    fun kmHistory(json: String): List<KmReading> = list(json) {
        KmReading(LocalDate.parse(it.getString("date")), it.getInt("km"))
    }

    fun accessToken(json: String): String = JSONObject(json).getString("accessToken")

    fun extensionId(json: String): String = JSONObject(json).getString("id")

    fun extensionBody(request: ExtensionRequest): JSONObject = JSONObject().apply {
        put("dueDate", request.dueDate.toString())
        request.newDate?.let { put("newDate", it.toString()) }
        put("excuse", request.excuse)
    }

    private fun <T> list(json: String, map: (JSONObject) -> T): List<T> {
        val array = JSONArray(json)
        return List(array.length()) { map(array.getJSONObject(it)) }
    }
}
