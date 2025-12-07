package com.example.caredose.repository

import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MasterVital
import kotlinx.coroutines.flow.Flow

class MasterVitalRepository(private val database: AppDatabase) {

    private val dao = database.masterVitalDao()

    // Get all vitals for a specific user
    fun getVitalsByUser(userId: Long): Flow<List<MasterVital>> {
        return dao.getVitalsByUser(userId)
    }

    // Insert a new vital
    suspend fun insertVital(vital: MasterVital): Long {
        return dao.insert(vital)
    }

    // Insert multiple vitals (optional usage)
    suspend fun insertAllVitals(vitals: List<MasterVital>) {
        dao.insertAll(vitals)
    }

    // Update a vital
    suspend fun updateVital(vital: MasterVital) {
        dao.update(vital)
    }

    // Delete a vital
    suspend fun deleteVital(vital: MasterVital) {
        dao.delete(vital)
    }

    // Get vital by ID
    suspend fun getVitalById(vitalId: Long): MasterVital? {
        return dao.getById(vitalId)
    }

    // Search by name
    suspend fun searchVitalByName(query: String): List<MasterVital> {
        return dao.searchByName(query)
    }

    // Check if vital name already exists
    suspend fun findVitalByName(name: String): MasterVital? {
        return dao.findByName(name)
    }
}
