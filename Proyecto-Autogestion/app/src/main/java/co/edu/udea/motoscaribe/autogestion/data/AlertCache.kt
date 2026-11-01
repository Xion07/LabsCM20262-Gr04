package co.edu.udea.motoscaribe.autogestion.data

import android.content.Context
import java.time.LocalDate

/**
 * Copia minima de la proxima cuota para que el trabajo de alertas funcione en segundo plano sin
 * token (el token solo vive en memoria y dura 15 min). Es privada de la app (sin backup) y se
 * borra al cerrar sesion.
 */
object AlertCache {
    private const val PREFS = "alert_cache"

    data class Snapshot(
        val dueDate: LocalDate,
        val amount: Long,
        val kmTraveled: Int,
        val lastOilChangeKm: Int?,
    )

    fun save(ctx: Context, c: Contract) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putLong("due", c.nextPaymentDate.toEpochDay())
            putLong("amount", c.nextPaymentAmount)
            putInt("km", c.kmTraveled)
            if (c.lastOilChangeKm != null) putInt("oil", c.lastOilChangeKm) else remove("oil")
        }.apply()
    }

    fun read(ctx: Context): Snapshot? {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("due")) return null
        return Snapshot(
            dueDate = LocalDate.ofEpochDay(p.getLong("due", 0)),
            amount = p.getLong("amount", 0),
            kmTraveled = p.getInt("km", 0),
            lastOilChangeKm = if (p.contains("oil")) p.getInt("oil", 0) else null,
        )
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
