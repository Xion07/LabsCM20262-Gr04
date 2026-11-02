package co.edu.udea.motoscaribe.autogestion.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonMappingTest {

    @Test fun contractWithNullOilChange() {
        val c = JsonMapping.contract(
            """{"holderName":"Camilo","plate":"ABC12D","totalValue":6800000,"balance":2450000,
               "nextPaymentDate":"2026-11-06","nextPaymentAmount":350000,"remainingMonths":14,
               "kmTraveled":2870,"lastOilChangeKm":null}"""
        )
        assertEquals("Camilo", c.holderName)
        assertEquals(LocalDate.of(2026, 11, 6), c.nextPaymentDate)
        assertEquals(2870, c.kmTraveled)
        assertNull(c.lastOilChangeKm)
    }

    @Test fun contractWithOilChange() {
        val c = JsonMapping.contract(
            """{"holderName":"A","plate":"B","totalValue":1,"balance":1,"nextPaymentDate":"2026-11-06",
               "nextPaymentAmount":1,"remainingMonths":1,"kmTraveled":10,"lastOilChangeKm":500}"""
        )
        assertEquals(500, c.lastOilChangeKm)
    }

    @Test fun unknownFineReasonDoesNotBreak() {
        val fines = JsonMapping.fines(
            """[{"date":"2026-09-01","amount":25000,"reason":"LATE_PAYMENT","installment":12},
                {"date":"2026-06-01","amount":10000,"reason":"ALGO_NUEVO","installment":5}]"""
        )
        assertEquals(FineReason.LATE_PAYMENT, fines[0].reason)
        assertEquals(FineReason.OTHER, fines[1].reason)
    }

    @Test fun listsAndToken() {
        assertEquals(2, JsonMapping.payments("""[{"date":"2026-10-25","amount":350000},{"date":"2026-10-18","amount":350000}]""").size)
        assertEquals(2870, JsonMapping.kmHistory("""[{"date":"2026-11-01","km":2870}]""")[0].km)
        assertEquals("abc", JsonMapping.accessToken("""{"accessToken":"abc","tokenType":"Bearer"}"""))
    }

    @Test fun extensionBodyOmitsNewDateWhenNull() {
        val body = JsonMapping.extensionBody(
            ExtensionRequest(LocalDate.of(2026, 11, 6), null, "Se dañó la moto", null)
        )
        assertEquals("2026-11-06", body.getString("dueDate"))
        assertEquals(false, body.has("newDate"))
    }

    @Test fun extensionHistoryMapsStatusAndFallsBack() {
        val list = JsonMapping.extensions(
            """[{"id":"a","dueDate":"2026-11-06","newDate":"2026-11-10","excuse":"Se dañó la moto","hasAttachment":true,
                 "status":"PENDING_REVIEW","createdAt":"2026-11-01T15:30:00Z"},
                {"id":"b","dueDate":"2026-11-06","newDate":null,"excuse":"Otra","hasAttachment":false,
                 "status":"ALGO_NUEVO","createdAt":"2026-11-02T15:30:00Z"}]"""
        )
        assertEquals(2, list.size)
        assertEquals(LocalDate.of(2026, 11, 10), list[0].newDate)
        assertEquals(true, list[0].hasAttachment)
        assertNull(list[1].newDate)
        assertEquals(RequestStatus.PENDING_REVIEW, list[1].status)
    }
}
