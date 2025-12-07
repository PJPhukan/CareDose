package com.example.caredose.repository

import com.example.caredose.database.dao.MedicineStockDao
import com.example.caredose.database.entities.MedicineStock
import kotlinx.coroutines.flow.Flow

class MedicineStockRepository(private val medicineStockDao: MedicineStockDao) {

    fun getStockByPatient(patientId: Long): Flow<List<MedicineStock>> {
        return medicineStockDao.getStockByPatient(patientId)
    }

    suspend fun getStockByPatientList(patientId: Long): List<MedicineStock> {
        return medicineStockDao.getStockByPatientList(patientId)
    }

    suspend fun insert(stock: MedicineStock): Long {
        return medicineStockDao.insert(stock)
    }

    suspend fun update(stock: MedicineStock) {
        medicineStockDao.update(stock)
    }

    suspend fun delete(stock: MedicineStock) {
        medicineStockDao.delete(stock)
    }

    suspend fun decrementStock(stockId: Long, qty: Int): Boolean {
        return medicineStockDao.decrementStock(stockId, qty) > 0
    }

    suspend fun incrementStock(stockId: Long, qty: Int) {
        medicineStockDao.incrementStock(stockId, qty)
    }

    suspend fun getLowStockForPatient(patientId: Long): List<MedicineStock> {
        return medicineStockDao.getLowStockForPatient(patientId)
    }
}