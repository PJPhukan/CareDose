package com.example.caredose.repository

import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Patient
import kotlinx.coroutines.flow.Flow

class PatientRepository(private val database: AppDatabase) {

    private val patientDao = database.patientDao()
    fun getPatientsByUser(userId: Long): Flow<List<Patient>> {
        return patientDao.getPatientsByUser(userId)
    }
    suspend fun getPatientsByUserList(userId: Long): List<Patient> {
        return patientDao.getPatientsByUserList(userId)
    }

    suspend fun getPatientById(id: Long): Patient? {
        return patientDao.getById(id)
    }
    suspend fun addPatient(patient: Patient): Long {
               return patientDao.insert(patient)
    }

       suspend fun updatePatient(patient: Patient) {
        patientDao.update(patient)
    }

    suspend fun deletePatient(patient: Patient) {
        patientDao.delete(patient)
    }

    suspend fun searchPatients(userId: Long, query: String): List<Patient> {
        return patientDao.searchPatients(userId, query)
    }
}