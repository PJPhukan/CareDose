package com.example.caredose.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresPermission
import com.example.caredose.database.entities.Dose
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit


class CareDoseAlarmDoseManager(private val context: Context) : AlarmSchedular {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun scheduleReminderDose(
        dose: Dose,
        medicineName: String,
        patientName: String,
        medicineId: Long
    ) {
        if (!dose.isActive) {
            return
        }

        if (dose.isTakenToday) {
            return
        }

        if (dose.isExpired()) {
            return
        }

        if (!dose.isValidSchedule()) {
            return
        }

        val doseHour = dose.timeInMinutes / 60
        val doseMinute = dose.timeInMinutes % 60
        val reminderBefore = dose.reminderMinutesBefore
        val alarmTime = calculateAlarmTime(doseHour, doseMinute, reminderBefore)

        if (dose.endDate != null && alarmTime > dose.endDate) {
            return
        }
        if (dose.endDate != null) {
            val daysRemaining =
                TimeUnit.MILLISECONDS.toDays(dose.endDate - System.currentTimeMillis())
        }
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
        } catch (e: SecurityException) {
            Log.e("Alarm", "Permission denied for dose ${dose.doseId}: ${e.message}", e)

        } catch (e: Exception) {
            Log.e("Alarm", "Error setting alarm for dose ${dose.doseId}: ${e.message}", e)
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
        } catch (e: Exception) {
            Log.e("Cancel", "Error cancelling alarm for dose ${dose.doseId}: ${e.message}", e)
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
        } catch (e: Exception) {
            Log.e("Cancel", "Error cancelling alarm for dose ID $doseId: ${e.message}", e)
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
    }

    val alarmMillis = actualAlarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

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
private fun formatTimestamp(timestampMillis: Long): String {
    val instant = Instant.ofEpochMilli(timestampMillis)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return dateTime.toString()
}

private fun formatEndDate(endDate: Long?): String {
    if (endDate == null) return "CONTINUOUS"
    val instant = Instant.ofEpochMilli(endDate)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    return dateTime.toString()
}