package com.example.caredose.repository

import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Patient
import kotlinx.coroutines.flow.Flow

class PatientRepository(private val database: AppDatabase) {

    private val patientDao = database.patientDao()

    /**
     * Get all patients for logged-in user (Flow for reactive UI)
     */
    fun getPatientsByUser(userId: Long): Flow<List<Patient>> {
        return patientDao.getPatientsByUser(userId)
    }

    /**
     * Get all patients for logged-in user (one-time fetch)
     */
    suspend fun getPatientsByUserList(userId: Long): List<Patient> {
        return patientDao.getPatientsByUserList(userId)
    }

    /**
     * Get patient by ID
     */
    suspend fun getPatientById(id: Long): Patient? {
        return patientDao.getById(id)
    }

    /**
     * Add new patient
     */
    suspend fun addPatient(patient: Patient): Long {
               return patientDao.insert(patient)
    }

    /**
     * Update existing patient
     */
    suspend fun updatePatient(patient: Patient) {
        patientDao.update(patient)
    }

    /**
     * Delete patient
     */
    suspend fun deletePatient(patient: Patient) {
        patientDao.delete(patient)
    }

    /**
     * Search patients by name
     */
    suspend fun searchPatients(userId: Long, query: String): List<Patient> {
        return patientDao.searchPatients(userId, query)
    }
}