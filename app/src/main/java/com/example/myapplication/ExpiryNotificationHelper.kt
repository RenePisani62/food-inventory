package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplication.data.AppDatabase
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import android.app.PendingIntent
import android.content.Intent

object ExpiryNotificationHelper {

    suspend fun checkAndNotify(context: Context) {
        val dao = AppDatabase.getDatabase(context).productDao()
        val items = dao.getAllProductsAlphabetical()

        val today = LocalDate.now()
        var expired = 0
        var expiringSoon = 0

        items.forEach { item ->
            val date = item.expiryDate?.trim()

            if (!date.isNullOrBlank()) {
                try {
                    val expiry = LocalDate.parse(date)
                    val days = ChronoUnit.DAYS.between(today, expiry)

                    when {
                        days < 0 -> expired++
                        days <= 7 -> expiringSoon++
                    }
                } catch (_: Exception) {
                }
            }
        }

        if (expired > 0 || expiringSoon > 0) {
            showNotification(context, expiringSoon, expired)
        }
    }

    private fun showNotification(
        context: Context,
        expiringSoon: Int,
        expired: Int
    ) {
        val channelId = "expiry_alerts"

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Expiry Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            manager.createNotificationChannel(channel)
        }

        val message = buildString {
            if (expiringSoon > 0) append("$expiringSoon expiring soon. ")
            if (expired > 0) append("$expired expired.")
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("PantryPal")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }
}