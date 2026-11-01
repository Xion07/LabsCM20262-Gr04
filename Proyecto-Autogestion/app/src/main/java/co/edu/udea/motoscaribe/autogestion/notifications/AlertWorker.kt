package co.edu.udea.motoscaribe.autogestion.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import co.edu.udea.motoscaribe.autogestion.data.MockCreditRepository
import co.edu.udea.motoscaribe.autogestion.domain.AlertPolicy
import co.edu.udea.motoscaribe.autogestion.domain.MaintenancePlan
import co.edu.udea.motoscaribe.autogestion.domain.MaintenanceStatus
import co.edu.udea.motoscaribe.autogestion.ui.cop
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private const val ID_PAYMENT = 1
private const val ID_MAINTENANCE = 2

/**
 * Revisa el contrato, notifica segun [AlertPolicy] y se vuelve a programar con
 * un intervalo mas corto mientras el pago no se registre (escalamiento).
 */
class AlertWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val contract = MockCreditRepository().contract() // pendiente: repositorio real
        val alert = AlertPolicy.evaluate(
            LocalDate.now(), contract.nextPaymentDate, contract.nextPaymentAmount.cop(),
        )
        alert?.let { Notifier.show(applicationContext, ID_PAYMENT, it.title, it.message, it.urgent) }

        val plan = contract.lastOilChangeKm?.let { MaintenancePlan(contract.kmTraveled, it) }
        if (plan != null && plan.status != MaintenanceStatus.OK) {
            val text = if (plan.status == MaintenanceStatus.OVERDUE) {
                "Te pasaste ${-plan.kmLeft} km del cambio de aceite."
            } else {
                "Te faltan ${plan.kmLeft} km para el cambio de aceite (${plan.nextServiceKm} km)."
            }
            Notifier.show(applicationContext, ID_MAINTENANCE, "Mantenimiento preventivo", text, false)
        }

        AlertScheduler.scheduleNext(applicationContext, alert?.nextCheckHours ?: AlertPolicy.IDLE_CHECK_HOURS)
        return Result.success()
    }
}

object AlertScheduler {
    private const val WORK_NAME = "payment-alerts"

    fun scheduleNext(ctx: Context, delayHours: Long) = enqueue(ctx, delayHours, TimeUnit.HOURS)

    /** Arranca la revision ya (al iniciar sesion). */
    fun start(ctx: Context) = enqueue(ctx, 0, TimeUnit.SECONDS)

    private fun enqueue(ctx: Context, delay: Long, unit: TimeUnit) {
        val request = OneTimeWorkRequestBuilder<AlertWorker>().setInitialDelay(delay, unit).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}
