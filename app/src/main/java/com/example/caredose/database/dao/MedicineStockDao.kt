package com.example.caredose.database.dao

import androidx.room.*
import com.example.caredose.database.entities.MedicineStock
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineStockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stock: MedicineStock): Long

    @Update
    suspend fun update(stock: MedicineStock)

    @Delete
    suspend fun delete(stock: MedicineStock)

    @Query("SELECT * FROM medicine_stock WHERE stockId = :id")
    suspend fun getById(id: Long): MedicineStock?

    @Query("SELECT * FROM medicine_stock WHERE patientId = :patientId")
    fun getStockByPatient(patientId: Long): Flow<List<MedicineStock>>

    @Query("SELECT * FROM medicine_stock WHERE patientId = :patientId")
    suspend fun getStockByPatientList(patientId: Long): List<MedicineStock>

    @Query("UPDATE medicine_stock SET stockQty = stockQty - :qty WHERE stockId = :stockId AND stockQty >= :qty")
    suspend fun decrementStock(stockId: Long, qty: Int): Int

    @Query("UPDATE medicine_stock SET stockQty = stockQty + :qty WHERE stockId = :stockId")
    suspend fun incrementStock(stockId: Long, qty: Int)

    @Query("SELECT * FROM medicine_stock WHERE stockQty <= reminderStockThreshold AND isReminderEnabled = 1")
    suspend fun getLowStockMedicines(): List<MedicineStock>

    @Query("SELECT * FROM medicine_stock WHERE patientId = :patientId AND stockQty <= reminderStockThreshold AND isReminderEnabled = 1")
    suspend fun getLowStockForPatient(patientId: Long): List<MedicineStock>
}