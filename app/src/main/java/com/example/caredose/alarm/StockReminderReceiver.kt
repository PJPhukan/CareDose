package com.example.caredose.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.caredose.MainActivity
import com.example.caredose.R
import com.example.caredose.SessionManager
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MedicineStock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.log


class StockReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STOCK_REMINDER = "com.example.caredose.STOCK_REMINDER"
        private const val CHANNEL_ID = "stock_reminder_channel"
        private const val CHANNEL_NAME = "Stock Reminders"
        private const val NOTIFICATION_ID = 200001
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_STOCK_REMINDER) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                checkAndNotifyLowStock(context)

                withContext(Dispatchers.Main) {
                    val scheduler = StockReminderScheduler(context)
                    scheduler.scheduleStockReminders()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Unable to checking stock",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun checkAndNotifyLowStock(context: Context) {
        val db = AppDatabase.getDatabase(context)

        val sessionManager = SessionManager(context)
        val userId = sessionManager.getUserId()

        if (userId == null) {
            Toast.makeText(
                context,
                "User not logged in, please logged in",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val lowStockMedicines = db.medicineStockDao().getLowStockForUser(userId)


        if (lowStockMedicines.isNotEmpty()) {
            val medicineDetails = mutableListOf<Pair<String, MedicineStock>>()
            lowStockMedicines.forEach { stock ->
                val medicine = db.masterMedicineDao().getById(stock.masterMedicineId)
                if (medicine != null) {
                    medicineDetails.add(Pair(medicine.name, stock))
                }
            }

            withContext(Dispatchers.Main) {
                showLowStockNotification(context, medicineDetails)
            }
        }
    }

    private fun showLowStockNotification(
        context: Context,
        medicineDetails: List<Pair<String, MedicineStock>>
    ) {
        createNotificationChannel(context)

        val count = medicineDetails.size
        val title = if (count == 1) {
            "⚠️ Low Stock Alert"
        } else {
            "⚠️ $count Medicines Low on Stock"
        }


        val contentText = if (count == 1) {
            val (name, stock) = medicineDetails[0]
            "$name: Only ${stock.stockQty} left (threshold: ${stock.reminderStockThreshold})"
        } else {
            "${medicineDetails[0].first} and ${count - 1} other medicine${if (count > 2) "s" else ""}"
        }

        val bigText = buildString {
            append("The following medicines are running low:\n\n")
            medicineDetails.forEachIndexed { index, (name, stock) ->
                append("${index + 1}. $name: ${stock.stockQty} left")
                if (stock.stockQty == 0) {
                    append(" OUT OF STOCK")
                } else {
                    append(" (threshold: ${stock.reminderStockThreshold})")
                }
                append("\n")
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_stock_screen", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 500, 200, 500))

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for low medicine stock alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}