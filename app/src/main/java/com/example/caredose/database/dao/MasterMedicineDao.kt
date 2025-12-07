package com.example.caredose.database.dao

import androidx.room.*
import com.example.caredose.database.entities.MasterMedicine
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterMedicineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: MasterMedicine): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medicines: List<MasterMedicine>)

    @Update
    suspend fun update(medicine: MasterMedicine)

    @Delete
    suspend fun delete(medicine: MasterMedicine)

    @Query("SELECT * FROM master_medicines ORDER BY name ASC")
    fun getAllFlow(): Flow<List<MasterMedicine>>

    @Query("SELECT * FROM master_medicines ORDER BY name ASC")
    suspend fun getAll(): List<MasterMedicine>

    @Query("SELECT * FROM master_medicines WHERE medicineId = :id")
    suspend fun getById(id: Long): MasterMedicine?

    // ADDED: Get medicine by ID (for repository)
    @Query("SELECT * FROM master_medicines WHERE medicineId = :medicineId")
    suspend fun getMedicineById(medicineId: Long): MasterMedicine?

    // ADDED: Get medicines by user ID
    @Query("SELECT * FROM master_medicines WHERE userId = :userId ORDER BY name ASC")
    fun getMedicinesByUser(userId: Long): Flow<List<MasterMedicine>>

    @Query("SELECT * FROM master_medicines WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchByName(query: String): List<MasterMedicine>

    @Query("SELECT * FROM master_medicines WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): MasterMedicine?
}