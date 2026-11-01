package co.edu.udea.motoscaribe.autogestion.notifications

import android.content.Context
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.Routes
import co.edu.udea.motoscaribe.autogestion.domain.AlertLevel
import co.edu.udea.motoscaribe.autogestion.domain.PaymentAlert

/** Arma los textos de una alerta con los recursos del idioma del sistema. */
object AlertTexts {
    fun title(ctx: Context, a: PaymentAlert): String = ctx.getString(
        when (a.level) {
            AlertLevel.UPCOMING -> R.string.notif_upcoming_title
            AlertLevel.DUE_TODAY -> R.string.notif_today_title
            AlertLevel.OVERDUE -> R.string.notif_overdue_title
        }
    )

    fun text(ctx: Context, a: PaymentAlert, amount: String): String = when (a.level) {
        AlertLevel.UPCOMING ->
            ctx.resources.getQuantityString(R.plurals.notif_upcoming_text, a.days, a.days, amount)
        AlertLevel.DUE_TODAY -> ctx.getString(R.string.notif_today_text, amount)
        AlertLevel.OVERDUE ->
            ctx.resources.getQuantityString(R.plurals.notif_overdue_text, a.days, a.days, amount)
    }

    /** Vence hoy o en mora: llevar directo a pedir plazo; si no, a Inicio. */
    fun route(a: PaymentAlert): String = if (a.urgent) Routes.EXTENSION else Routes.HOME
}
