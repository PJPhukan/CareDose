package com.example.caredose.repository

import com.example.caredose.database.dao.DoseDao
import com.example.caredose.database.entities.Dose
import kotlinx.coroutines.flow.Flow

class DoseRepository(private val doseDao: DoseDao) {

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
}