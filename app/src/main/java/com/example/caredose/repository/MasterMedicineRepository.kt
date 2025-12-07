package com.example.caredose.repository

import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MasterMedicine
import kotlinx.coroutines.flow.Flow

class MasterMedicineRepository(private val database: AppDatabase) {

    private val dao = database.masterMedicineDao()

    // Get all medicines for a user
    fun getMedicinesByUser(userId: Long): Flow<List<MasterMedicine>> {
        return dao.getMedicinesByUser(userId)
    }

    // Insert new medicine
    suspend fun insertMedicine(medicine: MasterMedicine): Long {
        return dao.insert(medicine)
    }



    // Update medicine
    suspend fun updateMedicine(medicine: MasterMedicine) {
        dao.update(medicine)
    }

    // Get medicine by ID
    suspend fun getMedicineById(medicineId: Long): MasterMedicine? {
        return dao.getById(medicineId)
    }

    // Delete medicine (for future use)
    suspend fun deleteMedicine(medicine: MasterMedicine) {
        dao.delete(medicine)
    }
}