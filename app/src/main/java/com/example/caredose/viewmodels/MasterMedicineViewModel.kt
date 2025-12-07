package com.example.caredose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.caredose.States
import com.example.caredose.database.entities.MasterMedicine
import com.example.caredose.repository.MasterMedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MasterMedicineViewModel(
    private val repository: MasterMedicineRepository
) : ViewModel() {

    private val _medicinesState = MutableStateFlow<States<List<MasterMedicine>>>(States.Idle)
    val medicinesState: StateFlow<States<List<MasterMedicine>>> = _medicinesState

    private val _addEditState = MutableStateFlow<States<Boolean>>(States.Idle)
    val addEditState: StateFlow<States<Boolean>> = _addEditState

    fun loadMedicines(userId: Long) {
        viewModelScope.launch {
            _medicinesState.value = States.Loading
            repository.getMedicinesByUser(userId)
                .catch { e ->
                    _medicinesState.value = States.Error(e.message ?: "Failed to load medicines")
                }
                .collect { medicines ->
                    _medicinesState.value = States.Success(medicines)
                }
        }
    }

    fun addMedicine(medicine: MasterMedicine) {
        viewModelScope.launch {
            try {
                _addEditState.value = States.Loading
                repository.insertMedicine(medicine)
                _addEditState.value = States.Success(true)
            } catch (e: Exception) {
                _addEditState.value = States.Error(e.message ?: "Failed to add medicine")
            }
        }
    }

    fun updateMedicine(medicineId: Long, userId: Long, name: String) {
        viewModelScope.launch {
            try {
                _addEditState.value = States.Loading
                val medicine = MasterMedicine(
                    medicineId = medicineId,
                    userId = userId,
                    name = name.trim()
                )
                repository.updateMedicine(medicine)
                _addEditState.value = States.Success(true)
            } catch (e: Exception) {
                _addEditState.value = States.Error(e.message ?: "Failed to update medicine")
            }
        }
    }

    suspend fun addMedicineAndGetId(medicine: MasterMedicine): Long {
        return repository.insertMedicine(medicine)
    }

    fun resetAddEditState() {
        _addEditState.value = States.Idle
    }
}