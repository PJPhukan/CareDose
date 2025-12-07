package com.example.caredose.database.dao

import androidx.room.*
import com.example.caredose.database.entities.MasterVital
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterVitalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vital: MasterVital): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vitals: List<MasterVital>)

    @Update
    suspend fun update(vital: MasterVital)

    @Delete
    suspend fun delete(vital: MasterVital)

    @Query("SELECT * FROM master_vitals ORDER BY name ASC")
    fun getAllFlow(): Flow<List<MasterVital>>

    @Query("SELECT * FROM master_vitals ORDER BY name ASC")
    suspend fun getAll(): List<MasterVital>

    @Query("SELECT * FROM master_vitals WHERE vitalId = :id")
    suspend fun getById(id: Long): MasterVital?

    // ADDED: Get vital by ID (for repository)
    @Query("SELECT * FROM master_vitals WHERE vitalId = :vitalId")
    suspend fun getVitalById(vitalId: Long): MasterVital?

    // ADDED: Get vitals by user ID
    @Query("SELECT * FROM master_vitals WHERE userId = :userId ORDER BY name ASC")
    fun getVitalsByUser(userId: Long): Flow<List<MasterVital>>

    @Query("SELECT * FROM master_vitals WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchByName(query: String): List<MasterVital>

    @Query("SELECT * FROM master_vitals WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): MasterVital?
}