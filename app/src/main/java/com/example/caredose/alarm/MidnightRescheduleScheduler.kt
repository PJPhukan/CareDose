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

            Log.d(TAG, "✅ Midnight reschedule alarm set successfully")
            Log.d(TAG, "   Next trigger: ${LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(midnightTime),
                ZoneId.systemDefault()
            )}")
            Log.d(TAG, "   Time until midnight: ${hours}h ${minutes}m")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission denied for scheduling exact alarm: ${e.message}", e)
            Log.e(TAG, "   Please enable SCHEDULE_EXACT_ALARM permission in settings")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error scheduling midnight reschedule: ${e.message}", e)
        }
    }

    /**
     * Cancels the scheduled midnight reschedule alarm
     */
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
            Log.d(TAG, "🚫 Midnight reschedule cancelled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling midnight reschedule: ${e.message}", e)
        }
    }

    /**
     * Calculates the timestamp for the next midnight (00:00:00)
     * If it's already past midnight today, it will schedule for tomorrow
     */
    private fun calculateNextMidnight(): Long {
        val now = LocalDateTime.now()

        // Calculate next midnight (tomorrow at 00:00:00)
        val midnight = now.plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        val midnightMillis = midnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        Log.d(TAG, "📅 Current time: $now")
        Log.d(TAG, "🌙 Next midnight: $midnight")

        return midnightMillis
    }

    /**
     * Checks if the midnight reschedule alarm is currently scheduled
     * Returns true if scheduled, false otherwise
     */
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