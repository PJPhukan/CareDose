package com.example.caredose.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId

private const val TAG = "MIDNIGHT_SCHEDULER"

class MidnightRescheduleScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    companion object {
        private const val MIDNIGHT_RESCHEDULE_REQUEST_CODE = 999999
    }

    fun scheduleMidnightReschedule() {
        val midnightTime = calculateNextMidnight()

        val intent = Intent(context, MidnightRescheduleReceiver::class.java).apply {
            action = MidnightRescheduleReceiver.ACTION_MIDNIGHT_RESCHEDULE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_RESCHEDULE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                midnightTime,
                pendingIntent
            )

            val timeUntilMidnight = (midnightTime - System.currentTimeMillis()) / 1000 / 60
            val hours = timeUntilMidnight / 60
            val minutes = timeUntilMidnight % 60

        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for scheduling exact alarm: ${e.message}", e)
            Log.e(TAG, "   Please enable SCHEDULE_EXACT_ALARM permission in settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling midnight reschedule: ${e.message}", e)
        }
    }

    fun cancelMidnightReschedule() {
        val intent = Intent(context, MidnightRescheduleReceiver::class.java).apply {
            action = MidnightRescheduleReceiver.ACTION_MIDNIGHT_RESCHEDULE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_RESCHEDULE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling midnight reschedule: ${e.message}", e)
        }
    }

    private fun calculateNextMidnight(): Long {
        val now = LocalDateTime.now()

        // Calculate next midnight
        val midnight = now.plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val midnightMillis = midnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return midnightMillis
    }

    fun isMidnightRescheduleScheduled(): Boolean {
        val intent = Intent(context, MidnightRescheduleReceiver::class.java).apply {
            action = MidnightRescheduleReceiver.ACTION_MIDNIGHT_RESCHEDULE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_RESCHEDULE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        return pendingIntent != null
    }
}