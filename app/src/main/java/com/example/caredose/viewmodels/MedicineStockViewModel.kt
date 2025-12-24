package com.example.caredose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.caredose.database.entities.MedicineStock
import com.example.caredose.repository.MedicineStockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class MedicineStockViewModel(
    private val repository: MedicineStockRepository
) : ViewModel() {

    private val _userId = MutableStateFlow(0L)
    val medicineStocks = _userId.flatMapLatest { userId ->
        repository.getStockByUser(userId)
    }.asLiveData()

    fun setUserId(userId: Long) {
        _userId.value = userId
    }

    fun addStock(stock: MedicineStock) {
        viewModelScope.launch {
            repository.insert(stock)
        }
    }

    fun updateStock(stock: MedicineStock) {
        viewModelScope.launch {
            repository.update(stock)
        }
    }

    fun deleteStock(stock: MedicineStock) {
        viewModelScope.launch {
            repository.delete(stock)
        }
    }

    fun incrementStock(stockId: Long, quantity: Int) {
        viewModelScope.launch {
            repository.incrementStock(stockId, quantity)
        }
    }

    fun decrementStock(
        stockId: Long,
        quantity: Int,
        onSuccess: () -> Unit = {},
        onFailed: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.decrementStock(stockId, quantity)
            if (result > 0) {
                onSuccess()
            } else {
                onFailed()
            }
        }
    }
}