package co.edu.udea.motoscaribe.autogestion.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import co.edu.udea.motoscaribe.autogestion.MainActivity
import co.edu.udea.motoscaribe.autogestion.R
import co.edu.udea.motoscaribe.autogestion.Routes

object Notifier {
    const val EXTRA_ROUTE = "route"
    private const val CH_REMINDER = "reminders"
    private const val CH_URGENT = "urgent"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CH_REMINDER, ctx.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CH_URGENT, ctx.getString(R.string.channel_urgent), NotificationManager.IMPORTANCE_HIGH,
            )
        )
    }

    /**
     * Muestra la notificacion. En pantalla bloqueada solo sale un titulo generico
     * (sin montos) y al tocarla abre la app en [route].
     */
    @SuppressLint("MissingPermission") // se comprueba con areNotificationsEnabled()
    fun show(ctx: Context, id: Int, title: String, text: String, urgent: Boolean, route: String = Routes.HOME) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        createChannels(ctx)
        val channel = if (urgent) CH_URGENT else CH_REMINDER
        val priority = if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT

        val publicVersion = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(ctx.getString(R.string.notif_public_title))
            .build()

        val builder = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(priority)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(open(ctx, id, route))
            .setAutoCancel(true)
        if (urgent) {
            builder.addAction(
                0, ctx.getString(R.string.notif_action_extension), open(ctx, id + 100, Routes.EXTENSION),
            )
        }
        nm.notify(id, builder.build())
    }

    private fun open(ctx: Context, requestCode: Int, route: String): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            ctx, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
