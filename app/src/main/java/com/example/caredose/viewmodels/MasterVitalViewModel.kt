package com.example.caredose.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.caredose.States
import com.example.caredose.database.entities.MasterVital
import com.example.caredose.repository.MasterVitalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MasterVitalViewModel(
    private val repository: MasterVitalRepository
) : ViewModel() {

    private val _vitalsState = MutableStateFlow<States<List<MasterVital>>>(States.Idle)
    val vitalsState: StateFlow<States<List<MasterVital>>> = _vitalsState

    private val _addEditState = MutableStateFlow<States<Boolean>>(States.Idle)
    val addEditState: StateFlow<States<Boolean>> = _addEditState

    private var _allVitals: LiveData<List<MasterVital>>? = null
    val allVitals: LiveData<List<MasterVital>>
        get() = _allVitals ?: throw IllegalStateException("Call loadVitalsLiveData first")


    fun loadVitals(userId: Long) {
        viewModelScope.launch {
            _vitalsState.value = States.Loading
            repository.getVitalsByUser(userId)
                .catch { e ->
                    _vitalsState.value = States.Error(e.message ?: "Failed to load vitals")
                }
                .collect { vitals ->
                    _vitalsState.value = States.Success(vitals)
                }
        }
    }

    fun loadVitalsLiveData(userId: Long) {
        _allVitals = repository.getVitalsByUser(userId).asLiveData()
    }


    fun addVital(vital: MasterVital) {
        viewModelScope.launch {
            try {
                repository.insertVital(vital)
            } catch (e: Exception) {
                Log.e("Addvitals", "addVital: ${e.message}", )
            }
        }
    }

    fun addVital(userId: Long, name: String, unit: String) {
        viewModelScope.launch {
            try {
                _addEditState.value = States.Loading
                val vital = MasterVital(
                    userId = userId,
                    name = name.trim(),
                    unit = unit.trim()
                )
                repository.insertVital(vital)
                _addEditState.value = States.Success(true)
            } catch (e: Exception) {
                _addEditState.value = States.Error(e.message ?: "Failed to add vital")
            }
        }
    }

    suspend fun addVitalAndGetId(vital: MasterVital): Long {
        return repository.insertVital(vital)
    }

    fun updateVital(vitalId: Long, userId: Long, name: String, unit: String) {
        viewModelScope.launch {
            try {
                _addEditState.value = States.Loading
                val vital = MasterVital(
                    vitalId = vitalId,
                    userId = userId,
                    name = name.trim(),
                    unit = unit.trim()
                )
                repository.updateVital(vital)
                _addEditState.value = States.Success(true)
            } catch (e: Exception) {
                _addEditState.value = States.Error(e.message ?: "Failed to update vital")
            }
        }
    }

    fun deleteVital(vital: MasterVital) {
        viewModelScope.launch {
            try {
                repository.deleteVital(vital)
            } catch (e: Exception) {
                // TODO: show toastHandle error
            }
        }
    }

    fun resetAddEditState() {
        _addEditState.value = States.Idle
    }
}