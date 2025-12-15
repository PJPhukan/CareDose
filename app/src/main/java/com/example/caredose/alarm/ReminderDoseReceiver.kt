package com.example.caredose.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.caredose.PatientDetailActivity
import com.example.caredose.R
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.DoseLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

private val TAG = "REMINDER DOSE RECEIVER"

class ReminderDoseReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DOSE_REMINDER = "com.example.caredose.DOSE_REMINDER"
        const val ACTION_MARK_TAKEN = "com.example.caredose.MARK_TAKEN"
        const val ACTION_ADD_STOCK = "com.example.caredose.ADD_STOCK"
        const val ACTION_STOP_ALARM = "com.example.caredose.STOP_ALARM"
        const val EXTRA_DOSE_ID = "dose_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_PATIENT_NAME = "patient_name"
        const val EXTRA_DOSE_TIME = "dose_time"
        const val EXTRA_QUANTITY = "quantity"
        const val EXTRA_PATIENT_ID = "patient_id"
        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_STOCK_ID = "stock_id"

        private const val CHANNEL_ID = "dose_reminder_channel"
        private const val CHANNEL_NAME = "Dose Reminders"

        private var mediaPlayer: MediaPlayer? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📬 onReceive called with action: ${intent.action}")

        when (intent.action) {
            ACTION_DOSE_REMINDER -> handleDoseReminder(context, intent)
            ACTION_MARK_TAKEN -> handleMarkTaken(context, intent)
            ACTION_ADD_STOCK -> handleAddStock(context, intent)
            ACTION_STOP_ALARM -> handleStopAlarm(context, intent)
            else -> Log.d(TAG, "⚠️ Wrong action received: ${intent.action}")
        }
    }

    private fun handleDoseReminder(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(EXTRA_DOSE_ID, -1)
        val medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME) ?: "Unknown Medicine"
        val patientName = intent.getStringExtra(EXTRA_PATIENT_NAME) ?: "Unknown Patient"
        val doseTime = intent.getStringExtra(EXTRA_DOSE_TIME) ?: "Unknown Time"
        val quantity = intent.getIntExtra(EXTRA_QUANTITY, 1)
        val patientId = intent.getLongExtra(EXTRA_PATIENT_ID, -1)
        val medicineId = intent.getLongExtra(EXTRA_MEDICINE_ID, -1)

        Log.d(TAG, "🔔 Dose Reminder Triggered:")
        Log.d(TAG, "   Dose ID: $doseId")
        Log.d(TAG, "   Medicine: $medicineName")
        Log.d(TAG, "   Patient: $patientName")
        Log.d(TAG, "   Time: $doseTime")
        Log.d(TAG, "   Quantity: $quantity")

        playAlarmSound(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dose = db.doseDao().getById(doseId)
                val stockId = dose?.stockId ?: -1L
                val medicineStock = db.medicineStockDao().getById(stockId)
                val stockQty = medicineStock?.stockQty ?: 0

                Log.d(TAG, "   Stock ID: $stockId")
                Log.d(TAG, "   Current Stock: $stockQty")

                withContext(Dispatchers.Main) {
                    showNotification(
                        context, doseId, medicineName, patientName, doseTime,
                        quantity, patientId, medicineId, stockQty, stockId
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking stock: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun playAlarmSound(context: Context) {
        try {
            stopAlarmSound()

            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(context, alarmUri)

            mediaPlayer?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = true
                start()
                Log.d(TAG, "🔊 Alarm sound started")
            }

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                stopAlarmSound()
                Log.d(TAG, "⏱️ Alarm auto-stopped after 1 minute")
            }, 60000)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error playing alarm: ${e.message}", e)
        }
    }

    private fun stopAlarmSound() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
            Log.d(TAG, "🔇 Alarm stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping alarm: ${e.message}", e)
        }
    }

    private fun handleStopAlarm(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(EXTRA_DOSE_ID, -1)
        Log.d(TAG, "⏹️ Stop alarm for dose: $doseId")

        stopAlarmSound()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(generateNotificationId(doseId))
    }

    private fun handleMarkTaken(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(EXTRA_DOSE_ID, -1)
        val quantity = intent.getIntExtra(EXTRA_QUANTITY, 1)
        val stockId = intent.getLongExtra(EXTRA_STOCK_ID, -1)

        Log.d(TAG, "✅ Mark as taken: dose $doseId, stock $stockId")

        stopAlarmSound()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(generateNotificationId(doseId))

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val medicineStock = db.medicineStockDao().getById(stockId)
                val currentStock = medicineStock?.stockQty ?: 0

                if (currentStock < quantity) {
                    Log.w(TAG, "⚠️ Insufficient stock")
                    return@launch
                }

                val doseLog = DoseLog(
                    doseId = doseId,
                    quantityTaken = quantity,
                    stockBefore = currentStock,
                    stockAfter = currentStock - quantity,
                    timestamp = Date().time
                )
                db.doseLogDao().insert(doseLog)
                db.doseDao().markAsTaken(doseId, Date().time)
                db.medicineStockDao().decrementStock(stockId, quantity)

                Log.d(TAG, "✅ Logged: $currentStock → ${currentStock - quantity}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error logging dose: ${e.message}", e)
            }
        }
    }

    private fun handleAddStock(context: Context, intent: Intent) {
        val patientId = intent.getLongExtra(EXTRA_PATIENT_ID, -1)
        val stockId = intent.getLongExtra(EXTRA_STOCK_ID, -1)
        val doseId = intent.getLongExtra(EXTRA_DOSE_ID, -1)

        Log.d(TAG, "➕ Add stock: patient $patientId, stock $stockId")

        stopAlarmSound()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(generateNotificationId(doseId))

        val activityIntent = Intent(context, PatientDetailActivity::class.java).apply {
            putExtra("patient_id", patientId)
            putExtra("open_tab", 1)
            putExtra("stock_id", stockId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(activityIntent)
    }

    private fun showNotification(
        context: Context,
        doseId: Long,
        medicineName: String,
        patientName: String,
        doseTime: String,
        quantity: Int,
        patientId: Long,
        medicineId: Long,
        currentStock: Int,
        stockId: Long
    ) {
        createNotificationChannel(context)

        val hasEnoughStock = currentStock >= quantity
        val notificationId = generateNotificationId(doseId)

        Log.d(TAG, "📱 Notification: ID=$notificationId, Stock=$currentStock/$quantity")

        val openAppIntent = Intent(context, PatientDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("patient_id", patientId)
            putExtra(EXTRA_DOSE_ID, doseId)
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            doseId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopAlarmIntent = Intent(context, ReminderDoseReceiver::class.java).apply {
            action = ACTION_STOP_ALARM
            putExtra(EXTRA_DOSE_ID, doseId)
        }

        val stopAlarmPendingIntent = PendingIntent.getBroadcast(
            context,
            (doseId * 3000).toInt(),
            stopAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setDeleteIntent(stopAlarmPendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(
                R.drawable.ic_check,
                "Stop Alarm",
                stopAlarmPendingIntent
            )

        if (hasEnoughStock) {
            val markTakenIntent = Intent(context, ReminderDoseReceiver::class.java).apply {
                action = ACTION_MARK_TAKEN
                putExtra(EXTRA_DOSE_ID, doseId)
                putExtra(EXTRA_PATIENT_ID, patientId)
                putExtra(EXTRA_MEDICINE_ID, medicineId)
                putExtra(EXTRA_STOCK_ID, stockId)
                putExtra(EXTRA_QUANTITY, quantity)
            }

            val markTakenPendingIntent = PendingIntent.getBroadcast(
                context,
                (doseId * 1000).toInt(),
                markTakenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder
                .setContentTitle("💊 Time to take medicine!")
                .setContentText("$patientName, take $quantity x $medicineName at $doseTime")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Hey $patientName! Please take $quantity x $medicineName at $doseTime\n\nStock: $currentStock available\n\nTap to open app")
                )
                .addAction(
                    R.drawable.ic_check,
                    "Mark as Taken",
                    markTakenPendingIntent
                )
        } else {
            val addStockIntent = Intent(context, ReminderDoseReceiver::class.java).apply {
                action = ACTION_ADD_STOCK
                putExtra(EXTRA_DOSE_ID, doseId)
                putExtra(EXTRA_PATIENT_ID, patientId)
                putExtra(EXTRA_MEDICINE_ID, medicineId)
                putExtra(EXTRA_STOCK_ID, stockId)
            }

            val addStockPendingIntent = PendingIntent.getBroadcast(
                context,
                (doseId * 2000).toInt(),
                addStockIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder
                .setContentTitle("⚠️ Low Stock Alert!")
                .setContentText("$patientName: Need $quantity x $medicineName but only $currentStock available")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("$patientName needs $quantity x $medicineName at $doseTime\n\n⚠️ Only $currentStock in stock. Need ${quantity - currentStock} more!\n\nTap to open app or add stock")
                )
                .addAction(
                    R.drawable.ic_add,
                    "Add Stock",
                    addStockPendingIntent
                )
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for medicine dose reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(null, null)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun generateNotificationId(doseId: Long): Int {
        return (doseId * 100).toInt() + (System.currentTimeMillis() % 10000).toInt()
    }
}