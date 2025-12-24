package com.example.caredose.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.caredose.database.AppDatabase
import com.example.caredose.alarm.CareDoseAlarmDoseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DailyDoseCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "DailyDoseCleanup"
        const val WORK_NAME = "daily_dose_cleanup"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
                       val db = AppDatabase.getDatabase(applicationContext)
            val doseDao = db.doseDao()

            doseDao.resetAllDoses()
            val expiredDoses = doseDao.getExpiredActiveDoses(System.currentTimeMillis())

            if (expiredDoses.isNotEmpty()) {
                val deactivatedCount = doseDao.deactivateExpiredDoses(System.currentTimeMillis())
                val alarmManager = CareDoseAlarmDoseManager(applicationContext)
                expiredDoses.forEach { dose ->
                    try {
                        alarmManager.cancelScheduleReminder(dose)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cancelling alarm for dose ${dose.doseId}: ${e.message}")
                    }
                }
            }

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during daily cleanup: ${e.message}", e)
            Result.retry()
        }
    }
}

object DailyCleanupScheduler {

    fun schedule(context: Context) {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<DailyDoseCleanupWorker>(
            24,
            java.util.concurrent.TimeUnit.HOURS
        )
            .setInitialDelay(
                calculateDelayUntilMidnight(),
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()

        androidx.work.WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                DailyDoseCleanupWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

    }

    fun cancel(context: Context) {
        androidx.work.WorkManager.getInstance(context)
            .cancelUniqueWork(DailyDoseCleanupWorker.WORK_NAME)
   }

    private fun calculateDelayUntilMidnight(): Long {
        val now = java.time.LocalDateTime.now()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val duration = java.time.Duration.between(now, midnight)
        return duration.toMillis()
    }
}