package com.example.caredose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.caredose.database.entities.Vital
import com.example.caredose.repository.VitalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class VitalViewModel(
    private val repository: VitalRepository
) : ViewModel() {

    private val _patientId = MutableStateFlow(0L)

    val vitals = _patientId.flatMapLatest { patientId ->
        repository.getVitalsByPatient(patientId)
    }.asLiveData()

    fun setPatientId(patientId: Long) {
        _patientId.value = patientId
    }

    fun addVital(vital: Vital) {
        viewModelScope.launch {
            repository.insert(vital)
        }
    }

    fun updateVital(vital: Vital) {
        viewModelScope.launch {
            repository.update(vital)
        }
    }

    fun deleteVital(vital: Vital) {
        viewModelScope.launch {
            repository.delete(vital)
        }
    }
}