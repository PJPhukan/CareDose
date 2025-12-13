package com.example.caredose.database.dao
import androidx.room.Dao
import androidx.room.*
import com.example.caredose.database.entities.DoseLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DoseLog): Long

    @Update
    suspend fun update(log: DoseLog)

    @Delete
    suspend fun delete(log: DoseLog)

    @Query("SELECT * FROM dose_logs WHERE logId = :id")
    suspend fun getById(id: Long): DoseLog?

    @Query("SELECT * FROM dose_logs WHERE doseId = :doseId ORDER BY timestamp DESC")
    fun getLogsByDose(doseId: Long): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE doseId = :doseId ORDER BY timestamp DESC")
    suspend fun getLogsByDoseList(doseId: Long): List<DoseLog>

    @Query("""
        SELECT * FROM dose_logs 
        WHERE doseId = :doseId 
        AND timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """)
    suspend fun getLogsByDateRange(doseId: Long, startTime: Long, endTime: Long): List<DoseLog>

    @Query("""
        SELECT dl.* FROM dose_logs dl
        INNER JOIN doses d ON dl.doseId = d.doseId
        WHERE d.stockId = :stockId
        ORDER BY dl.timestamp DESC
    """)
    fun getLogsByMedicineStock(stockId: Long): Flow<List<DoseLog>>
}