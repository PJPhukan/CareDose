package com.example.caredose.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.caredose.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MIDNIGHT_RECEIVER"

class MidnightRescheduleReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MIDNIGHT_RESCHEDULE = "com.example.caredose.MIDNIGHT_RESCHEDULE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MIDNIGHT_RESCHEDULE) {
            return
        }

        Log.d(TAG, "🌙 Midnight reached - starting daily tasks...")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                performMidnightTasks(context)

                // Reschedule for next midnight
                val scheduler = MidnightRescheduleScheduler(context)
                scheduler.scheduleMidnightReschedule()
            } catch (e: Exception) {
                Log.e(TAG, "Error during midnight tasks: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun performMidnightTasks(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val alarmManager = CareDoseAlarmDoseManager(context)
        val currentTime = System.currentTimeMillis()

        db.doseDao().resetAllDoses()
        val expiredDoses = db.doseDao().getExpiredActiveDoses(currentTime)

        if (expiredDoses.isNotEmpty()) {
          val deactivatedCount = db.doseDao().deactivateExpiredDoses(currentTime)

            expiredDoses.forEach { dose ->
                try {
                    alarmManager.cancelScheduleReminder(dose)
                } catch (e: Exception) {
                    Log.e(TAG, "Error cancelling alarm for dose ${dose.doseId}: ${e.message}")
                }
            }
        }

        val validDoses = db.doseDao().getAllValidDosesForReminders(currentTime)

        var successCount = 0
        var failureCount = 0

        validDoses.forEach { dose ->
            try {
                if (dose.isValidSchedule() && !dose.isTakenToday) {
                    val medicine = db.medicineStockDao().getById(dose.stockId)
                    val patient = db.patientDao().getById(dose.patientId)
                    val masterMedicine = medicine?.let {
                        db.masterMedicineDao().getById(it.masterMedicineId)
                    }

                    if (patient != null && masterMedicine != null && medicine != null) {
                        alarmManager.scheduleReminderDose(
                            dose,
                            masterMedicine.name,
                            patient.name,
                            medicine.masterMedicineId
                        )
                        successCount++
                    } else {
                        failureCount++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling dose ${dose.doseId}: ${e.message}")
                failureCount++
            }
        }

    }
}