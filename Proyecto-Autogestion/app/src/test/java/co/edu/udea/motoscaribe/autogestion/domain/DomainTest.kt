package co.edu.udea.motoscaribe.autogestion.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainTest {
    private val today = LocalDate.of(2026, 11, 1)

    private fun alert(daysToDue: Long) =
        AlertPolicy.evaluate(today, today.plusDays(daysToDue), "$ 350.000")

    @Test fun noAlertWhenFarFromDue() = assertNull(alert(4))

    @Test fun upcomingAlertIsNotUrgent() {
        val a = alert(3)!!
        assertEquals(AlertLevel.UPCOMING, a.level)
        assertTrue(!a.urgent)
    }

    @Test fun dueTodayIsUrgent() {
        val a = alert(0)!!
        assertEquals(AlertLevel.DUE_TODAY, a.level)
        assertTrue(a.urgent)
    }

    @Test fun overdueEscalatesFrequency() {
        val upcoming = alert(2)!!.nextCheckHours
        val dueToday = alert(0)!!.nextCheckHours
        val overdue = alert(-2)!!
        assertEquals(AlertLevel.OVERDUE, overdue.level)
        assertTrue(upcoming > dueToday && dueToday > overdue.nextCheckHours)
    }

    @Test fun maintenanceStatusByKm() {
        assertEquals(MaintenanceStatus.OK, MaintenancePlan(1000, 0).status)
        assertEquals(MaintenanceStatus.DUE_SOON, MaintenancePlan(2870, 0).status)
        assertEquals(MaintenanceStatus.OVERDUE, MaintenancePlan(3100, 0).status)
    }

    @Test fun maintenanceProgressIsClamped() {
        assertEquals(1f, MaintenancePlan(9000, 0).progress, 0f)
        assertEquals(130, MaintenancePlan(2870, 0).kmLeft)
    }
}
