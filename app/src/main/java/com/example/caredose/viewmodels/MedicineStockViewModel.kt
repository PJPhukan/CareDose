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

    private val _patientId = MutableStateFlow(0L)

    val medicineStocks = _patientId.flatMapLatest { patientId ->
        repository.getStockByPatient(patientId)
    }.asLiveData()

    fun setPatientId(patientId: Long) {
        _patientId.value = patientId
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

    fun incrementStock(stockId: Long, qty: Int) {
        viewModelScope.launch {
            repository.incrementStock(stockId, qty)
        }
    }

    fun decrementStock(stockId: Long, qty: Int, onSuccess: () -> Unit, onFailed: () -> Unit) {
        viewModelScope.launch {
            val success = repository.decrementStock(stockId, qty)
            if (success) onSuccess() else onFailed()
        }
    }
}