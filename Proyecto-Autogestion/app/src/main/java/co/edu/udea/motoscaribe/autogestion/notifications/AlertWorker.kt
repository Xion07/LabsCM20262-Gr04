package co.edu.udea.motoscaribe.autogestion.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.Routes
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
        val ctx = applicationContext
        val contract = MockCreditRepository().contract() // pendiente: repositorio real
        val alert = AlertPolicy.evaluate(LocalDate.now(), contract.nextPaymentDate)
        alert?.let {
            Notifier.show(
                ctx, ID_PAYMENT,
                AlertTexts.title(ctx, it),
                AlertTexts.text(ctx, it, contract.nextPaymentAmount.cop()),
                it.urgent,
                AlertTexts.route(it),
            )
        }

        val plan = contract.lastOilChangeKm?.let { MaintenancePlan(contract.kmTraveled, it) }
        if (plan != null && plan.status != MaintenanceStatus.OK) {
            val text = if (plan.status == MaintenanceStatus.OVERDUE) {
                ctx.getString(R.string.notif_oil_over, -plan.kmLeft)
            } else {
                ctx.getString(R.string.notif_oil_left, plan.kmLeft, plan.nextServiceKm)
            }
            Notifier.show(ctx, ID_MAINTENANCE, ctx.getString(R.string.notif_oil_title), text, false, Routes.KM)
        }

        AlertScheduler.scheduleNext(ctx, alert?.nextCheckHours ?: AlertPolicy.IDLE_CHECK_HOURS)
        return Result.success()
    }
}

object AlertScheduler {
    private const val WORK_NAME = "payment-alerts"

    /** Reprograma la siguiente revision (reemplaza la pendiente). */
    fun scheduleNext(ctx: Context, delayHours: Long) =
        enqueue(ctx, delayHours, TimeUnit.HOURS, ExistingWorkPolicy.REPLACE)

    /** Arranca la revision ya; si ya hay una programada se conserva (rotar no la reinicia). */
    fun start(ctx: Context) = enqueue(ctx, 0, TimeUnit.SECONDS, ExistingWorkPolicy.KEEP)

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
    }

    private fun enqueue(ctx: Context, delay: Long, unit: TimeUnit, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<AlertWorker>().setInitialDelay(delay, unit).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(WORK_NAME, policy, request)
    }
}
