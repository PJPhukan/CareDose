package com.example.caredose.repository

import com.example.caredose.database.dao.MedicineStockDao
import com.example.caredose.database.entities.MedicineStock
import kotlinx.coroutines.flow.Flow

class MedicineStockRepository(private val dao: MedicineStockDao) {

    // Changed from patient-based to user-based
    fun getStockByUser(userId: Long): Flow<List<MedicineStock>> {
        return dao.getStockByUser(userId)
    }

    suspend fun getStockByUserList(userId: Long): List<MedicineStock> {
        return dao.getStockByUserList(userId)
    }

    suspend fun getStockByMedicineId(medicineId: Long, userId: Long): MedicineStock? {
        return dao.getStockByMedicineId(medicineId, userId)
    }

    suspend fun insert(stock: MedicineStock): Long {
        return dao.insert(stock)
    }

    suspend fun update(stock: MedicineStock) {
        dao.update(stock)
    }

    suspend fun delete(stock: MedicineStock) {
        dao.delete(stock)
    }

    // Fixed: Actually decrement (not increment)
    suspend fun decrementStock(stockId: Long, quantity: Int): Int {
        return dao.decrementStock(stockId, quantity)
    }

    suspend fun incrementStock(stockId: Long, quantity: Int) {
        dao.incrementStock(stockId, quantity)
    }

    suspend fun getLowStockMedicines(): List<MedicineStock> {
        return dao.getLowStockMedicines()
    }

    suspend fun getLowStockForUser(userId: Long): List<MedicineStock> {
        return dao.getLowStockForUser(userId)
    }
}