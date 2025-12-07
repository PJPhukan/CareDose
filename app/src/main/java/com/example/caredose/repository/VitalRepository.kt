package com.example.caredose.repository

import com.example.caredose.database.dao.VitalDao
import com.example.caredose.database.entities.Vital
import kotlinx.coroutines.flow.Flow

class VitalRepository(private val vitalDao: VitalDao) {

    fun getVitalsByPatient(patientId: Long): Flow<List<Vital>> {
        return vitalDao.getVitalsByPatient(patientId)
    }

    fun getVitalsByType(patientId: Long, vitalTypeId: Long): Flow<List<Vital>> {
        return vitalDao.getVitalsByType(patientId, vitalTypeId)
    }

    suspend fun insert(vital: Vital): Long {
        return vitalDao.insert(vital)
    }

    suspend fun update(vital: Vital) {
        vitalDao.update(vital)
    }

    suspend fun delete(vital: Vital) {
        vitalDao.delete(vital)
    }

    suspend fun getVitalsInRange(patientId: Long, startTime: Long, endTime: Long): List<Vital> {
        return vitalDao.getVitalsInRange(patientId, startTime, endTime)
    }
}