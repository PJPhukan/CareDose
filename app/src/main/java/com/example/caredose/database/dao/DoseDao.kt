package com.example.caredose.database.dao

import androidx.room.*
import com.example.caredose.database.entities.Dose
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

    @Query("""
        SELECT * FROM doses 
        WHERE scheduleGroupId = :scheduleGroupId 
        ORDER BY timeInMinutes ASC
    """)
    suspend fun getDosesByScheduleGroup(scheduleGroupId: String): List<Dose>

    @Query("""
        SELECT * FROM doses 
        WHERE scheduleGroupId = :scheduleGroupId 
        ORDER BY timeInMinutes ASC
    """)
    fun getDosesByScheduleGroupFlow(scheduleGroupId: String): Flow<List<Dose>>

    @Query("""
        SELECT * FROM doses 
        WHERE patientId = :patientId 
        AND isActive = 1 
        AND (endDate IS NULL OR endDate > :currentTime)
        ORDER BY timeInMinutes ASC
    """)
    fun getActiveDosesForPatient(patientId: Long, currentTime: Long): Flow<List<Dose>>

    @Query("""
        SELECT * FROM doses 
        WHERE patientId = :patientId 
        AND isActive = 1 
        AND (endDate IS NULL OR endDate > :currentTime)
        ORDER BY timeInMinutes ASC
    """)
    suspend fun getActiveDosesForPatientList(patientId: Long, currentTime: Long): List<Dose>

    @Query("""
        SELECT * FROM doses 
        WHERE isActive = 1 
        AND endDate IS NOT NULL 
        AND endDate <= :currentTime
    """)
    suspend fun getExpiredActiveDoses(currentTime: Long): List<Dose>

    @Query("""
        UPDATE doses 
        SET isActive = 0 
        WHERE isActive = 1 
        AND endDate IS NOT NULL 
        AND endDate <= :currentTime
    """)
    suspend fun deactivateExpiredDoses(currentTime: Long): Int

     @Query("""
        SELECT DISTINCT scheduleGroupId 
        FROM doses 
        WHERE patientId = :patientId 
        AND isActive = 1
    """)
    suspend fun getScheduleGroupsByPatient(patientId: Long): List<String>

    @Query("DELETE FROM doses WHERE scheduleGroupId = :scheduleGroupId")
    suspend fun deleteScheduleGroup(scheduleGroupId: String)

    @Query("""
        UPDATE doses 
        SET isActive = 0 
        WHERE scheduleGroupId = :scheduleGroupId
    """)
    suspend fun deactivateScheduleGroup(scheduleGroupId: String)

    @Query("""
        UPDATE doses 
        SET durationType = :durationType,
            durationValue = :durationValue,
            endDate = :endDate
        WHERE scheduleGroupId = :scheduleGroupId
    """)
    suspend fun updateScheduleGroupDuration(
        scheduleGroupId: String,
        durationType: String,
        durationValue: Int?,
        endDate: Long?
    )

    @Query("""
        SELECT COUNT(*) FROM doses 
        WHERE patientId = :patientId 
        AND isActive = 1 
        AND (endDate IS NULL OR endDate > :currentTime)
    """)
    suspend fun countActiveDoses(patientId: Long, currentTime: Long): Int

    @Query("""
        SELECT * FROM doses 
        WHERE patientId = :patientId 
        AND isActive = 1 
        AND endDate IS NOT NULL 
        AND endDate > :currentTime 
        AND endDate <= :sevenDaysFromNow
        ORDER BY endDate ASC
    """)
    suspend fun getDosesExpiringSoon(
        patientId: Long,
        currentTime: Long,
        sevenDaysFromNow: Long
    ): List<Dose>

    @Query("""
        SELECT * FROM doses 
        WHERE isActive = 1 
        AND (endDate IS NULL OR endDate > :currentTime)
    """)
    suspend fun getAllValidDosesForReminders(currentTime: Long): List<Dose>
}