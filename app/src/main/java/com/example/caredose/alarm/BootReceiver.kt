package com.example.caredose.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.caredose.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAllAlarms(context)
            } catch (e: Exception) {
                Toast.makeText(context, " ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun rescheduleAllAlarms(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val alarmManager = CareDoseAlarmDoseManager(context)

        val currentTime = System.currentTimeMillis()
        val doses = db.doseDao().getAllValidDosesForReminders(currentTime)

        var successCount = 0
        var skippedCount = 0

        doses.forEach { dose ->
            try {
                if (!dose.isValidSchedule()) {
                    skippedCount++
                    return@forEach
                }

                val medicine = db.medicineStockDao().getById(dose.stockId)
                val patient = db.patientDao().getById(dose.patientId)
                val masterMedicine = medicine?.let {
                    db.masterMedicineDao().getById(it.masterMedicineId)
                }

                if (patient != null && masterMedicine != null) {
                    alarmManager.scheduleReminderDose(
                        dose,
                        masterMedicine.name,
                        patient.name,
                        medicine.masterMedicineId
                    )
                    successCount++
                } else {
                    skippedCount++
                }
            } catch (e: Exception) {
                skippedCount++
            }
        }

        val midnightScheduler = MidnightRescheduleScheduler(context)
        midnightScheduler.scheduleMidnightReschedule()

        val stockReminderScheduler = StockReminderScheduler(context)
        stockReminderScheduler.scheduleStockReminders()
    }
}