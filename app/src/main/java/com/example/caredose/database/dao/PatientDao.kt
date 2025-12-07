package com.example.caredose.database.dao

import androidx.room.*
import androidx.room.Dao
import com.example.caredose.database.entities.Patient
import kotlinx.coroutines.flow.Flow


@Dao
interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patient: Patient): Long

    @Update
    suspend fun update(patient: Patient)

    @Delete
    suspend fun delete(patient: Patient)

    @Query("SELECT * FROM patients WHERE patientId = :id")
    suspend fun getById(id: Long): Patient?

    // Get all patients for logged-in user (userId is now Long)
    @Query("SELECT * FROM patients WHERE userId = :userId ORDER BY name ASC")
    fun getPatientsByUser(userId: Long): Flow<List<Patient>>

    @Query("SELECT * FROM patients WHERE userId = :userId ORDER BY name ASC")
    suspend fun getPatientsByUserList(userId: Long): List<Patient>

    // Search patients by name
    @Query("SELECT * FROM patients WHERE userId = :userId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchPatients(userId: Long, query: String): List<Patient>
}