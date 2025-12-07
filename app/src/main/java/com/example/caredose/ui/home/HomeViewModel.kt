package com.example.caredose.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.caredose.States
import com.example.caredose.database.entities.Patient
import com.example.caredose.repository.PatientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: PatientRepository? = null) : ViewModel() {
    //------PATIENT----
    private val _patients = MutableStateFlow<States<List<Patient>>>(States.Idle)
    val patients: StateFlow<States<List<Patient>>> = _patients.asStateFlow()

    private val _patientOperation = MutableStateFlow<States<Long>>(States.Idle)
    val patientOperation: StateFlow<States<Long>> = _patientOperation.asStateFlow()

    //LOAD ALL USERS
    fun loadPatients(userId: Long) {
        viewModelScope.launch {
            _patients.value = States.Loading
            try {
                repository?.getPatientsByUser(userId)?.collect { patientList ->
                    _patients.value = States.Success(patientList)
                }
            } catch (e: Exception) {
                _patients.value = States.Error(e.message ?: "Failed to load patients")
            }
        }
    }

    //ADD NEW PATIENT
    fun addPatient(userId: Long, name: String, age: Int, gender: String) {
        viewModelScope.launch {
            _patientOperation.value = States.Loading
            try {
                val patient = Patient(userId, name, age, gender)
                val patientId = repository?.addPatient(patient)
                _patientOperation.value = States.Success(patientId) as States<Long>
            } catch (e: Exception) {
                _patientOperation.value = States.Error(e.message ?: "Failed to add patient")
            }
        }
    }

  //UPDATE PATIENT DETAILS
    fun updatePatient(patient: Patient) {
        viewModelScope.launch {
            _patientOperation.value = States.Loading
            try {
                repository?.updatePatient(patient)
                _patientOperation.value = States.Success(patient.patientId)
            } catch (e: Exception) {
                _patientOperation.value = States.Error(e.message ?: "Failed to update patient")
            }
        }
    }

    //DELETE PATIENT
    fun deletePatient(patient: Patient) {
        viewModelScope.launch {
            _patientOperation.value = States.Loading
            try {
                repository?.deletePatient(patient)
                _patientOperation.value = States.Success(patient.patientId)
            } catch (e: Exception) {
                _patientOperation.value = States.Error(e.message ?: "Failed to delete patient")
            }
        }
    }


    fun resetOperationState() {
        _patientOperation.value = States.Idle
    }


}