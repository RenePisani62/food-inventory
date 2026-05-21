package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.AppDatabase
import java.time.LocalDate
import java.time.temporal.ChronoUnit



class ExpiryNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        ExpiryNotificationHelper.checkAndNotify(applicationContext)
        return Result.success()
    }


    private fun showNotification(expiringSoon: Int, expired: Int) {
        val channelId = "expiry_alerts"

        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("PantryPal")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(1001, notification)
    }
}