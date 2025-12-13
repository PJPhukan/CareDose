package com.example.caredose.repository

import com.example.caredose.database.dao.DoseDao
import com.example.caredose.database.dao.DoseLogDao // ASSUMED: You need a DAO for logging
import com.example.caredose.database.dao.MedicineStockDao
import com.example.caredose.database.entities.Dose
import com.example.caredose.database.entities.DoseLog // ASSUMED: You need a DoseLog entity
import kotlinx.coroutines.flow.Flow

class DoseRepository(
    private val doseDao: DoseDao,
    // FIX 1: Inject MedicineStockDao and DoseLogDao instances
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

    // FIX 2: Un-nested functions
    suspend fun resetAllDoses() {
        doseDao.resetAllDoses()
    }

    // FIX 2: Un-nested functions
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
}