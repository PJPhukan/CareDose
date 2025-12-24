package com.example.caredose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.caredose.States
import com.example.caredose.database.entities.Patient
import com.example.caredose.repository.PatientRepository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PatientViewModel(
    private val patientRepository: PatientRepository
) : ViewModel() {

    // StateFlow for patients list
    private val _patientsState = MutableStateFlow<States<List<Patient>>>(States.Idle)
    val patientsState: StateFlow<States<List<Patient>>> = _patientsState

    private val _operationState = MutableStateFlow<States<Unit>>(States.Idle)
    val operationState: StateFlow<States<Unit>> = _operationState


    fun loadPatients(userId: Long) {
        viewModelScope.launch {
            _patientsState.value = States.Loading
            try {
                patientRepository.getPatientsByUser(userId).collect { patients ->
                    _patientsState.value = States.Success(patients)
                }
            } catch (e: Exception) {
                _patientsState.value = States.Error(e.message ?: "Failed to load patients")
            }
        }
    }

    fun addPatient(patient: Patient) {
        viewModelScope.launch {
            _operationState.value = States.Loading
            try {
                patientRepository.addPatient(patient)
                _operationState.value = States.Success(Unit)
            } catch (e: Exception) {
                _operationState.value = States.Error(e.message ?: "Failed to add patient")
            }
        }
    }

    fun updatePatient(patient: Patient) {
        viewModelScope.launch {
            _operationState.value = States.Loading
            try {
                patientRepository.updatePatient(patient)
                _operationState.value = States.Success(Unit)
            } catch (e: Exception) {
                _operationState.value = States.Error(e.message ?: "Failed to update patient")
            }
        }
    }

    fun deletePatient(patient: Patient) {
        viewModelScope.launch {
            _operationState.value = States.Loading
            try {
                patientRepository.deletePatient(patient)
                _operationState.value = States.Success(Unit)
            } catch (e: Exception) {
                _operationState.value = States.Error(e.message ?: "Failed to delete patient")
            }
        }
    }


    fun resetOperationState() {
        _operationState.value = States.Idle
    }
}