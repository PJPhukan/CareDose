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

    @Query("SELECT * FROM medicine_stock WHERE userId = :userId")
    fun getStockByUser(userId: Long): Flow<List<MedicineStock>>

    @Query("SELECT * FROM medicine_stock WHERE userId = :userId")
    suspend fun getStockByUserList(userId: Long): List<MedicineStock>

    @Query("SELECT * FROM medicine_stock WHERE masterMedicineId = :medicineId AND userId = :userId")
    suspend fun getStockByMedicineId(medicineId: Long, userId: Long): MedicineStock?

    @Query("UPDATE medicine_stock SET stockQty = stockQty - :qty WHERE stockId = :stockId AND stockQty >= :qty")
    suspend fun decrementStock(stockId: Long, qty: Int): Int

    @Query("UPDATE medicine_stock SET stockQty = stockQty - :qty WHERE masterMedicineId = :medicineId AND userId = :userId AND stockQty >= :qty")
    suspend fun decrementStockByMedicineId(medicineId: Long, qty: Int, userId: Long): Int

    @Query("UPDATE medicine_stock SET stockQty = stockQty + :qty WHERE stockId = :stockId")
    suspend fun incrementStock(stockId: Long, qty: Int)

    @Query("SELECT * FROM medicine_stock WHERE stockQty <= reminderStockThreshold AND isReminderEnabled = 1")
    suspend fun getLowStockMedicines(): List<MedicineStock>


    @Query("SELECT * FROM medicine_stock WHERE userId = :userId AND stockQty <= reminderStockThreshold AND isReminderEnabled = 1")
    suspend fun getLowStockForUser(userId: Long): List<MedicineStock>

    @Query("SELECT * FROM medicine_stock WHERE masterMedicineId = :medicineId")
    fun getMedicineSync(medicineId: Long): MedicineStock?

    @Query("SELECT * FROM medicine_stock WHERE stockId = :stockId")
    fun getMedicineByStockIdSync(stockId: Long): MedicineStock?

    @Query("SELECT * FROM medicine_stock WHERE userId = :userId")
    fun getMedicineStocksForUserSync(userId: Long): List<MedicineStock>
}