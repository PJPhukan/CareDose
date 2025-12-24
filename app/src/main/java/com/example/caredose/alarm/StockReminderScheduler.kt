package com.example.caredose.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.log
class StockReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    companion object {
        private const val MORNING_HOUR = 8
        private const val MORNING_MINUTE = 30
        private const val EVENING_HOUR = 21
        private const val EVENING_MINUTE = 38

        private const val MORNING_REQUEST_CODE = 100001
        private const val EVENING_REQUEST_CODE = 100002
    }

    fun scheduleStockReminders() {
        scheduleMorningReminder()
        scheduleEveningReminder()
    }

    private fun scheduleMorningReminder() {
        val alarmTime = calculateNextAlarmTime(MORNING_HOUR, MORNING_MINUTE)
        scheduleAlarm(alarmTime, MORNING_REQUEST_CODE, "Morning")
    }

    private fun scheduleEveningReminder() {
        val alarmTime = calculateNextAlarmTime(EVENING_HOUR, EVENING_MINUTE)
        scheduleAlarm(alarmTime, EVENING_REQUEST_CODE, "Evening")
    }

    private fun scheduleAlarm(alarmTime: Long, requestCode: Int, label: String) {
        val intent = Intent(context, StockReminderReceiver::class.java).apply {
            action = StockReminderReceiver.ACTION_STOCK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )


            val nextTrigger = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(alarmTime),
                ZoneId.systemDefault()
            )
        } catch (e: SecurityException) {
            Toast.makeText(
                context,
                "Permission denied. Please allow alarm & notification permissions.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to schedule $label reminder. Please try again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun calculateNextAlarmTime(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var alarmTime = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        if (alarmTime.isBefore(now) || alarmTime.isEqual(now)) {
            alarmTime = alarmTime.plusDays(1)
        }
        return alarmTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun areStockRemindersScheduled(): Boolean {
        val morningIntent = Intent(context, StockReminderReceiver::class.java).apply {
            action = StockReminderReceiver.ACTION_STOCK_REMINDER
        }

        val morningPendingIntent = PendingIntent.getBroadcast(
            context,
            MORNING_REQUEST_CODE,
            morningIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        return morningPendingIntent != null
    }
}