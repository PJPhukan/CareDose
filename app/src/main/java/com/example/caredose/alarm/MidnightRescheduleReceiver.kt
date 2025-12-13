package com.example.caredose.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.caredose.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MIDNIGHT_RESCHEDULE"

class MidnightRescheduleReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MIDNIGHT_RESCHEDULE = "com.example.caredose.MIDNIGHT_RESCHEDULE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MIDNIGHT_RESCHEDULE) {
            Log.d(TAG, "Wrong action received: ${intent.action}")
            return
        }

        Log.d(TAG, "⏰ Midnight reschedule triggered at ${System.currentTimeMillis()}")
        Log.d(TAG, "📅 Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")

        // Use goAsync() to allow background work
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAllDoses(context)

                scheduleMidnightReschedule(context)

                Log.d(TAG, "✅ All doses rescheduled successfully for new day")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error rescheduling doses: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAllDoses(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val alarmManager = CareDoseAlarmDoseManager(context)

        // Get all active doses
        val doses = db.doseDao().getAllActiveDoses()

        Log.d(TAG, "📋 Found ${doses.size} total doses")

        var rescheduledCount = 0

        doses.forEach { dose ->
            if (dose.isActive) {
                try {
                    // Get medicine and patient info
                    val stock = db.medicineStockDao().getById(dose.stockId)
                    val patient = db.patientDao().getById(dose.patientId)
                    val medicine = db.masterMedicineDao().getById(stock!!.masterMedicineId)

                    if (medicine != null && patient != null) {
                        alarmManager.scheduleReminderDose(dose, medicine.name, patient.name,medicine.medicineId)
                        rescheduledCount++
                        Log.d(TAG, "✅ Rescheduled dose ${dose.doseId} for ${patient.name} - ${medicine.name}")
                    } else {
                        Log.w(TAG, "⚠️ Skipped dose ${dose.doseId}: Medicine or Patient not found")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error rescheduling dose ${dose.doseId}: ${e.message}", e)
                }
            }
        }

        Log.d(TAG, "📊 Rescheduled $rescheduledCount active doses out of ${doses.size} total doses")
    }

    private fun scheduleMidnightReschedule(context: Context) {
        val scheduler = MidnightRescheduleScheduler(context)
        scheduler.scheduleMidnightReschedule()
    }
}