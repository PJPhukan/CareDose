package com.example.caredose.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.caredose.database.entities.Dose
import java.time.LocalDateTime
import java.time.ZoneId

private val TAG = "CARE DOSE ALARM DOSE MANAGER"

class CareDoseAlarmDoseManager(private val context: Context) : AlarmSchedular {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun scheduleReminderDose(
        dose: Dose,
        medicineName: String,
        patientName: String,
        medicineId:Long
    ) {
        // Check if dose reminder is active or not
        if (!dose.isActive) {
            Log.d(TAG, "scheduleReminderDose: Dose ${dose.doseId} is not active")
            return
        }

        // Get times from the dose
        val doseHour = dose.timeInMinutes / 60
        val doseMinute = dose.timeInMinutes % 60
        val reminderBefore = dose.reminderMinutesBefore

        val alarmTime = calculateAlarmTime(doseHour, doseMinute, reminderBefore)
        Log.d(TAG, "ALARM TIME : $alarmTime for dose ${dose.doseId}")

        val intent = Intent(context, ReminderDoseReceiver::class.java).apply {
            action = ReminderDoseReceiver.ACTION_DOSE_REMINDER
            putExtra(ReminderDoseReceiver.EXTRA_DOSE_ID, dose.doseId)
            putExtra(ReminderDoseReceiver.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(ReminderDoseReceiver.EXTRA_PATIENT_NAME, patientName)
            putExtra(
                ReminderDoseReceiver.EXTRA_DOSE_TIME,
                formatTimeWithAmPm(doseHour, doseMinute)

            )
            putExtra(ReminderDoseReceiver.EXTRA_QUANTITY, dose.quantity)
            // ADD THESE IMPORTANT EXTRAS FOR STOCK CHECKING
            putExtra(ReminderDoseReceiver.EXTRA_PATIENT_ID, dose.patientId)
            putExtra(ReminderDoseReceiver.EXTRA_MEDICINE_ID, medicineId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            dose.doseId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule alarm
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
            Log.d(TAG, "✅ Successfully scheduled alarm for dose ${dose.doseId}")
            Log.d(TAG, "   Patient: $patientName")
            Log.d(TAG, "   Medicine: $medicineName")
            Log.d(TAG, "   Time: ${String.format("%02d:%02d", doseHour, doseMinute)}")
            Log.d(TAG, "   Reminder before: $reminderBefore minutes")
        } catch (e: SecurityException) {
            Log.e(
                TAG, "❌ Permission denied for dose ${dose.doseId}: ${e.message}", e
            )
        } catch (e: Exception) {
            Log.e(
                TAG, "❌ Error setting alarm for dose ${dose.doseId}: ${e.message}",
                e
            )
        }
    }


    override fun cancelScheduleReminder(dose: Dose) {
        val intent = Intent(context, ReminderDoseReceiver::class.java).apply {
            action = ReminderDoseReceiver.ACTION_DOSE_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            dose.doseId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "🚫 Cancelled alarm for dose ${dose.doseId}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling alarm for dose ${dose.doseId}: ${e.message}", e)
        }
    }


    override fun cancelReminderByDoseId(doseId: Long) {
        val intent = Intent(context, ReminderDoseReceiver::class.java).apply {
            action = ReminderDoseReceiver.ACTION_DOSE_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            doseId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "🚫 Cancelled alarm for dose ID: $doseId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling alarm for dose ID $doseId: ${e.message}", e)
        }
    }
}

private fun calculateAlarmTime(
    doseHour: Int,
    doseMinute: Int,
    reminderBeforeMinutes: Int
): Long {
    val now = LocalDateTime.now()
    val doseTime = LocalDateTime.now()
        .withHour(doseHour)
        .withMinute(doseMinute)
        .withSecond(0)
        .withNano(0)
    var actualAlarmTime = doseTime.minusMinutes(reminderBeforeMinutes.toLong())

    if (actualAlarmTime.isBefore(now) || actualAlarmTime.isEqual(now)) {
        actualAlarmTime = actualAlarmTime.plusDays(1)
        Log.d(TAG, "⏰ Alarm time has passed today. Scheduling for TOMORROW")
    } else {
        Log.d(TAG, "⏰ Scheduling for TODAY")
    }

    val alarmMillis = actualAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    Log.d(TAG, "📅 Alarm scheduled for: $actualAlarmTime")

    return alarmMillis
}

private fun formatTimeWithAmPm(hour: Int, minute: Int): String {
    val period = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%02d:%02d %s", displayHour, minute, period)
}