package com.example.caredose.database.dao

import androidx.room.*
import com.example.caredose.database.entities.Dose
import com.example.caredose.database.entities.Vital
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dose: Dose): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(doses: List<Dose>)

    @Update
    suspend fun update(dose: Dose)

    @Delete
    suspend fun delete(dose: Dose)

    @Query("SELECT * FROM doses WHERE doseId = :id")
    suspend fun getById(id: Long): Dose?

    @Query("SELECT * FROM doses WHERE stockId = :stockId ORDER BY timeInMinutes ASC")
    fun getDosesByMedicineStock(stockId: Long): Flow<List<Dose>>

    @Query("SELECT * FROM doses WHERE patientId = :patientId ORDER BY timeInMinutes ASC")
    fun getDosesByPatient(patientId: Long): Flow<List<Dose>>

    @Query("DELETE FROM doses WHERE stockId = :stockId")
    suspend fun deleteAllForMedicineStock(stockId: Long)

    @Query("UPDATE doses SET isTakenToday = 0")
    suspend fun resetAllDoses()

    @Query("SELECT * FROM doses WHERE isActive = 1")
    suspend fun getAllActiveDoses(): List<Dose>

    @Query("UPDATE doses SET isTakenToday = 1, lastTakenAt = :timestamp WHERE doseId = :doseId")
    suspend fun markAsTaken(doseId: Long, timestamp: Long)
    @Query("DELETE FROM doses WHERE patientId = :patientId")
    suspend fun deleteAllForPatient(patientId: Long)


}