package com.example.caredose.repository

import com.example.caredose.database.dao.DoseDao
import com.example.caredose.database.dao.DoseLogDao
import com.example.caredose.database.dao.MedicineStockDao
import com.example.caredose.database.entities.Dose
import com.example.caredose.database.entities.DoseLog
import kotlinx.coroutines.flow.Flow

class DoseRepository(
    private val doseDao: DoseDao,
    private val medicineStockDao: MedicineStockDao,
    private val doseLogDao: DoseLogDao
) {


    fun getDosesByMedicineStock(stockId: Long): Flow<List<Dose>> {
        return doseDao.getDosesByMedicineStock(stockId)
    }

    fun getDosesByPatient(patientId: Long): Flow<List<Dose>> {
        return doseDao.getDosesByPatient(patientId)
    }

    suspend fun insert(dose: Dose): Long {
        return doseDao.insert(dose)
    }

    suspend fun insertAll(doses: List<Dose>) {
        doseDao.insertAll(doses)
    }

    suspend fun update(dose: Dose) {
        doseDao.update(dose)
    }

    suspend fun delete(dose: Dose) {
        doseDao.delete(dose)
    }

    suspend fun deleteAllForMedicineStock(stockId: Long) {
        doseDao.deleteAllForMedicineStock(stockId)
    }

    suspend fun resetAllDoses() {
        doseDao.resetAllDoses()
    }

    suspend fun getAllActiveDoses(): List<Dose> {
        return doseDao.getAllActiveDoses()
    }

    suspend fun markDoseTaken(dose: Dose): Boolean {
        return try {
            val stock = medicineStockDao.getById(dose.stockId) ?: return false
            val stockBefore = stock.stockQty

            if (stockBefore < dose.quantity) {
                return false
            }

            val timestamp = System.currentTimeMillis()

            doseDao.markAsTaken(dose.doseId, timestamp)

            val rowsAffected = medicineStockDao.decrementStock(dose.stockId, dose.quantity)

            if (rowsAffected > 0) {
                val updatedStock = medicineStockDao.getById(dose.stockId)

                val stockAfter = updatedStock?.stockQty ?: (stockBefore - dose.quantity)

                val log = DoseLog(
                    doseId = dose.doseId,
                    quantityTaken = dose.quantity,
                    stockBefore = stockBefore,
                    stockAfter = stockAfter,
                    timestamp = timestamp
                )
                doseLogDao.insert(log)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getDosesByScheduleGroup(scheduleGroupId: String): Flow<List<Dose>> {
        return doseDao.getDosesByScheduleGroupFlow(scheduleGroupId)
    }

    suspend fun getDosesByScheduleGroupOnce(scheduleGroupId: String): List<Dose> {
        return doseDao.getDosesByScheduleGroup(scheduleGroupId)
    }

    fun getValidDosesByPatient(patientId: Long): Flow<List<Dose>> {
        val currentTime = System.currentTimeMillis()
        return doseDao.getActiveDosesForPatient(patientId, currentTime)
    }

    suspend fun getDosesForAlarmScheduling(): List<Dose> {
        val currentTime = System.currentTimeMillis()
        return doseDao.getAllValidDosesForReminders(currentTime)
    }


    suspend fun deleteScheduleGroup(scheduleGroupId: String) {
        doseDao.deleteScheduleGroup(scheduleGroupId)
    }

    suspend fun updateScheduleGroupDuration(
        scheduleGroupId: String,
        durationType: String,
        durationValue: Int?,
        endDate: Long?
    ) {
        doseDao.updateScheduleGroupDuration(
            scheduleGroupId,
            durationType,
            durationValue,
            endDate
        )
    }

    suspend fun deactivateExpiredDoses(): Int {
        val currentTime = System.currentTimeMillis()
        return doseDao.deactivateExpiredDoses(currentTime)
    }

    suspend fun getExpiredDoses(): List<Dose> {
        val currentTime = System.currentTimeMillis()
        return doseDao.getExpiredActiveDoses(currentTime)
    }

    suspend fun getActiveScheduleCount(patientId: Long): Int {
        val currentTime = System.currentTimeMillis()
        return doseDao.countActiveDoses(patientId, currentTime)
    }

    suspend fun getExpiringDosesCount(patientId: Long, daysAhead: Int): Int {
      val currentTime = System.currentTimeMillis()
        val futureTime = currentTime + (daysAhead * 24 * 60 * 60 * 1000L)
        val doses = doseDao.getDosesExpiringSoon(patientId, currentTime, futureTime)
        return doses.size
    }

    suspend fun insertScheduleGroup(doses: List<Dose>): Boolean {
        return try {
            if (doses.isEmpty()) return false

            val scheduleGroupId = doses.first().scheduleGroupId
            if (doses.any { it.scheduleGroupId != scheduleGroupId }) {
                throw IllegalArgumentException("All doses must have the same scheduleGroupId")
            }

            doseDao.insertAll(doses)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateScheduleGroup(
        oldScheduleGroupId: String,
        newDoses: List<Dose>
    ): Boolean {
        return try {
            doseDao.deleteScheduleGroup(oldScheduleGroupId)
            doseDao.insertAll(newDoses)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}