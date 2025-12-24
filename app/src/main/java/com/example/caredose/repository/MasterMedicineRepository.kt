package com.example.caredose.repository

import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MasterMedicine
import kotlinx.coroutines.flow.Flow

class MasterMedicineRepository(private val database: AppDatabase) {

    private val dao = database.masterMedicineDao()

    fun getMedicinesByUser(userId: Long): Flow<List<MasterMedicine>> {
        return dao.getMedicinesByUser(userId)
    }

    suspend fun insertMedicine(medicine: MasterMedicine): Long {
        return dao.insert(medicine)
    }

    suspend fun updateMedicine(medicine: MasterMedicine) {
        dao.update(medicine)
    }

    suspend fun getMedicineById(medicineId: Long): MasterMedicine? {
        return dao.getById(medicineId)
    }

    suspend fun deleteMedicine(medicine: MasterMedicine) {
        dao.delete(medicine)
    }
}