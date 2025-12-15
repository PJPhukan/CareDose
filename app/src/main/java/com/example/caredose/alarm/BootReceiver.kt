package com.example.caredose.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.caredose.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "BOOT_RECEIVER"

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        Log.d(TAG, "📱 Device booted, rescheduling all alarms...")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAllAlarms(context)
                Log.d(TAG, "✅ All alarms rescheduled after boot")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error rescheduling alarms after boot: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAllAlarms(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val alarmManager = CareDoseAlarmDoseManager(context)

        // Reschedule all active doses
        val doses = db.doseDao().getAllActiveDoses()

        doses.forEach { dose ->
            if (dose.isActive) {
                val medicine = db.medicineStockDao().getById(dose.stockId)
                val patient = db.patientDao().getById(dose.patientId)
                val masterMedicine = db.masterMedicineDao().getById(medicine!!.masterMedicineId)
                if (patient != null) {
                    alarmManager.scheduleReminderDose(dose, masterMedicine!!.name, patient.name, medicine.masterMedicineId)
                    Log.d(TAG, "✅ Rescheduled dose ${dose.doseId}")
                }
            }
        }

        // Reschedule midnight reschedule
        val midnightScheduler = MidnightRescheduleScheduler(context)
        midnightScheduler.scheduleMidnightReschedule()

        // ✅ NEW: Reschedule stock reminders
        val stockReminderScheduler = StockReminderScheduler(context)
        stockReminderScheduler.scheduleStockReminders()
        Log.d(TAG, "✅ Stock reminders rescheduled")
    }
}