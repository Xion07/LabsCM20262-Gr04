package co.edu.udea.motoscaribe.autogestion.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    private const val CH_REMINDER = "reminders"
    private const val CH_URGENT = "urgent"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDER, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_URGENT, "Vencimiento y mora", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    @SuppressLint("MissingPermission") // se comprueba con areNotificationsEnabled()
    fun show(ctx: Context, id: Int, title: String, text: String, urgent: Boolean) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        createChannels(ctx)
        val n = NotificationCompat.Builder(ctx, if (urgent) CH_URGENT else CH_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        nm.notify(id, n)
    }
}
