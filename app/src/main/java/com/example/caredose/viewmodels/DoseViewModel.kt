package com.example.caredose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.caredose.database.entities.Dose
import com.example.caredose.repository.DoseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DoseViewModel(
    private val repository: DoseRepository
) : ViewModel() {

    private val _patientId = MutableStateFlow(0L)

    val doses = _patientId.flatMapLatest { patientId ->
        repository.getDosesByPatient(patientId)
    }.asLiveData()

    fun setPatientId(patientId: Long) {
        _patientId.value = patientId
    }

    fun addDose(dose: Dose):Long {
       return runBlocking {
           repository.insert(dose)
       }
    }

    fun addMultipleDoses(doses: List<Dose>) {
        viewModelScope.launch {
            repository.insertAll(doses)
        }
    }

    fun updateDose(dose: Dose) {
        viewModelScope.launch {
            repository.update(dose)
        }
    }

    fun markDoseTaken(dose: Dose) {
        viewModelScope.launch {
            repository.markDoseTaken(dose)
        }
    }

    fun deleteDose(dose: Dose) {
        viewModelScope.launch {
            repository.delete(dose)
        }
    }
}
