package com.example.caredose.repository

import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MasterVital
import kotlinx.coroutines.flow.Flow

class MasterVitalRepository(private val database: AppDatabase) {

    private val dao = database.masterVitalDao()

    fun getVitalsByUser(userId: Long): Flow<List<MasterVital>> {
        return dao.getVitalsByUser(userId)
    }

    suspend fun insertVital(vital: MasterVital): Long {
        return dao.insert(vital)
    }

    suspend fun insertAllVitals(vitals: List<MasterVital>) {
        dao.insertAll(vitals)
    }
    suspend fun updateVital(vital: MasterVital) {
        dao.update(vital)
    }
    suspend fun deleteVital(vital: MasterVital) {
        dao.delete(vital)
    }

      suspend fun getVitalById(vitalId: Long): MasterVital? {
        return dao.getById(vitalId)
    }

    suspend fun searchVitalByName(query: String): List<MasterVital> {
        return dao.searchByName(query)
    }

    suspend fun findVitalByName(name: String): MasterVital? {
        return dao.findByName(name)
    }
}
