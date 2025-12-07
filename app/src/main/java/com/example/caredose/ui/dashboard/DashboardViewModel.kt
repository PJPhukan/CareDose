package com.example.caredose.ui.dashboard

import androidx.lifecycle.*
import com.example.caredose.States
import com.example.caredose.database.entities.MasterVital
import com.example.caredose.database.entities.Patient
import com.example.caredose.database.entities.Vital
import com.example.caredose.repository.MasterVitalRepository
import com.example.caredose.repository.PatientRepository
import com.example.caredose.repository.VitalRepository
import kotlinx.coroutines.launch

data class VitalStats(
    val min: Double,
    val max: Double,
    val avg: Double
)

class DashboardViewModel(
    private val patientRepository: PatientRepository,
    private val vitalRepository: VitalRepository,
    private val masterVitalRepository: MasterVitalRepository
) : ViewModel() {

    private val _state = MutableLiveData<States<Unit>>(States.Idle)
    val state: LiveData<States<Unit>> = _state

    // User ID for filtering patients
    private val _userId = MutableLiveData<Long>()

    // Patients LiveData (user-specific)
    val patients: LiveData<List<Patient>> = _userId.switchMap { userId ->
        patientRepository.getPatientsByUser(userId).asLiveData()
    }

    // Selected patient and vital type IDs
    private val _selectedPatientId = MutableLiveData<Long?>()
    private val _selectedVitalTypeId = MutableLiveData<Long?>()

    // Vital types for selected patient
    private val _vitalTypes = MutableLiveData<List<MasterVital>>()
    val vitalTypes: LiveData<List<MasterVital>> = _vitalTypes

    // Vitals data for graph
    private val _vitalsData = MutableLiveData<List<Vital>>()
    val vitalsData: LiveData<List<Vital>> = _vitalsData

    // Statistics
    private val _stats = MutableLiveData<VitalStats?>()
    val stats: LiveData<VitalStats> = _stats as LiveData<VitalStats>

    // Selected entities (computed)
    val selectedPatient: LiveData<Patient?> = _selectedPatientId.switchMap { patientId ->
        if (patientId == null) {
            MutableLiveData(null)
        } else {
            patients.map { list ->
                list.find { it.patientId == patientId }
            }
        }
    }

    val selectedVitalType: LiveData<MasterVital?> = _selectedVitalTypeId.switchMap { vitalTypeId ->
        if (vitalTypeId == null) {
            MutableLiveData(null)
        } else {
            vitalTypes.map { list ->
                list.find { it.vitalId == vitalTypeId }
            }
        }
    }

    // Set user ID (call this from Fragment)
    fun setUserId(userId: Long) {
        if (_userId.value != userId) {
            _userId.value = userId
        }
    }

    // Select patient and load vital types
    fun selectPatient(patientId: Long) {
        if (_selectedPatientId.value != patientId) {
            _selectedPatientId.value = patientId
            _selectedVitalTypeId.value = null
            _vitalsData.value = emptyList()
            _stats.value = null

            // Load vital types for this patient
            loadVitalTypesForPatient(patientId)
        }
    }

    private fun loadVitalTypesForPatient(patientId: Long) {
        viewModelScope.launch {
            try {
                _state.value = States.Loading

                // Collect vitals as Flow
                vitalRepository.getVitalsByPatient(patientId).collect { vitals ->
                    if (vitals.isEmpty()) {
                        _vitalTypes.value = emptyList()
                        _state.value = States.Error("No vitals recorded for this patient")
                        return@collect
                    }

                    // Get unique vital type IDs
                    val uniqueVitalTypeIds = vitals.map { it.masterVitalId }.distinct()
                    loadMasterVitals(uniqueVitalTypeIds)
                }
            } catch (e: Exception) {
                _state.value = States.Error("Failed to load vitals: ${e.message}")
            }
        }
    }

    private suspend fun loadMasterVitals(vitalTypeIds: List<Long>) {
        try {
            val vitalTypes = vitalTypeIds.mapNotNull { id ->
                masterVitalRepository.getVitalById(id)
            }

            _vitalTypes.postValue(vitalTypes)
            _state.postValue(States.Success(Unit))

        } catch (e: Exception) {
            _state.postValue(States.Error("Failed to load vital types: ${e.message}"))
        }
    }

    fun selectVitalType(vitalTypeId: Long) {
        if (_selectedVitalTypeId.value != vitalTypeId) {
            _selectedVitalTypeId.value = vitalTypeId
        }
    }

    fun loadVitalsData(dateRangeDays: Int) {
        val patientId = _selectedPatientId.value ?: return
        val vitalTypeId = _selectedVitalTypeId.value ?: return

        viewModelScope.launch {
            try {
                _state.value = States.Loading

                // Calculate date range
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (dateRangeDays * 24 * 60 * 60 * 1000L)

                // Get vitals in range
                val allVitals = vitalRepository.getVitalsInRange(patientId, startTime, endTime)
                val filteredVitals = allVitals
                    .filter { it.masterVitalId == vitalTypeId }
                    .sortedBy { it.recordedAt }

                if (filteredVitals.isEmpty()) {
                    _vitalsData.value = emptyList()
                    _stats.value = null
                    _state.value = States.Error("No data available for selected period")
                    return@launch
                }

                // Calculate stats
                val values = filteredVitals.map { it.value }
                val stats = VitalStats(
                    min = values.minOrNull() ?: 0.0,
                    max = values.maxOrNull() ?: 0.0,
                    avg = if (values.isNotEmpty()) values.average() else 0.0
                )

                _vitalsData.value = filteredVitals
                _stats.value = stats
                _state.value = States.Success(Unit)

            } catch (e: Exception) {
                _state.value = States.Error("Failed to load vitals: ${e.message}")
            }
        }
    }

    fun clearError() {
        _state.value = States.Idle
    }
}