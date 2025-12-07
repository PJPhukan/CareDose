package com.example.caredose.database.dao

import androidx.room.*
import com.example.caredose.database.entities.Vital
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vital: Vital): Long

    @Update
    suspend fun update(vital: Vital)

    @Delete
    suspend fun delete(vital: Vital)

    @Query("SELECT * FROM vitals WHERE vitalRecordId = :id")
    suspend fun getById(id: Long): Vital?

    @Query("SELECT * FROM vitals WHERE patientId = :patientId ORDER BY recordedAt DESC")
    fun getVitalsByPatient(patientId: Long): Flow<List<Vital>>

    @Query("SELECT * FROM vitals WHERE patientId = :patientId AND masterVitalId = :vitalTypeId ORDER BY recordedAt DESC")
    fun getVitalsByType(patientId: Long, vitalTypeId: Long): Flow<List<Vital>>

    @Query("SELECT * FROM vitals WHERE patientId = :patientId AND recordedAt BETWEEN :startTime AND :endTime ORDER BY recordedAt ASC")
    suspend fun getVitalsInRange(patientId: Long, startTime: Long, endTime: Long): List<Vital>
    @Query("DELETE FROM vitals WHERE patientId = :patientId")
    suspend fun deleteAllForPatient(patientId: Long)
}