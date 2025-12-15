package com.example.caredose.ui.dashboard

import android.util.Log
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

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    private val _state = MutableLiveData<States<Unit>>(States.Idle)
    val state: LiveData<States<Unit>> = _state

    // User ID for filtering patients
    private val _userId = MutableLiveData<Long>()

    // Patients LiveData (user-specific)
    val patients: LiveData<List<Patient>> = _userId.switchMap { userId ->
        Log.d(TAG, "Loading patients for userId: $userId")
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
            Log.d(TAG, "setUserId: $userId")
            _userId.value = userId
        }
    }

    // Select patient and load vital types
    fun selectPatient(patientId: Long) {
        Log.d(TAG, "selectPatient called: $patientId")
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
        Log.d(TAG, "loadVitalTypesForPatient: $patientId")
        viewModelScope.launch {
            try {
                _state.value = States.Loading

                // Collect vitals as Flow
                vitalRepository.getVitalsByPatient(patientId).collect { vitals ->
                    Log.d(TAG, "Received ${vitals.size} vitals for patient $patientId")

                    if (vitals.isEmpty()) {
                        _vitalTypes.value = emptyList()
                        _state.value = States.Error("No vitals recorded for this patient")
                        return@collect
                    }

                    // Get unique vital type IDs
                    val uniqueVitalTypeIds = vitals.map { it.masterVitalId }.distinct()
                    Log.d(TAG, "Unique vital type IDs: $uniqueVitalTypeIds")
                    loadMasterVitals(uniqueVitalTypeIds)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading vital types: ${e.message}", e)
                _state.value = States.Error("Failed to load vitals: ${e.message}")
            }
        }
    }

    private suspend fun loadMasterVitals(vitalTypeIds: List<Long>) {
        try {
            val vitalTypes = vitalTypeIds.mapNotNull { id ->
                masterVitalRepository.getVitalById(id)
            }

            Log.d(TAG, "Loaded ${vitalTypes.size} master vitals")
            _vitalTypes.postValue(vitalTypes)
            _state.postValue(States.Success(Unit))

        } catch (e: Exception) {
            Log.e(TAG, "Error loading master vitals: ${e.message}", e)
            _state.postValue(States.Error("Failed to load vital types: ${e.message}"))
        }
    }

    fun selectVitalType(vitalTypeId: Long) {
        Log.d(TAG, "selectVitalType called: $vitalTypeId")
        // ✅ Always update the value, even if it's the same
        // This allows reloading data when user selects the same vital again
        _selectedVitalTypeId.value = vitalTypeId
    }

    fun loadVitalsData(dateRangeDays: Int) {
        val patientId = _selectedPatientId.value
        val vitalTypeId = _selectedVitalTypeId.value

        Log.d(TAG, "loadVitalsData called: dateRangeDays=$dateRangeDays, patientId=$patientId, vitalTypeId=$vitalTypeId")

        if (patientId == null) {
            Log.w(TAG, "Cannot load vitals: patientId is null")
            return
        }

        if (vitalTypeId == null) {
            Log.w(TAG, "Cannot load vitals: vitalTypeId is null")
            return
        }

        viewModelScope.launch {
            try {
                _state.value = States.Loading

                // Calculate date range
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (dateRangeDays * 24 * 60 * 60 * 1000L)

                Log.d(TAG, "Date range: startTime=$startTime, endTime=$endTime")
                Log.d(TAG, "Loading vitals for patientId=$patientId, vitalTypeId=$vitalTypeId, days=$dateRangeDays")

                // Get vitals in range
                val allVitals = vitalRepository.getVitalsInRange(patientId, startTime, endTime)
                Log.d(TAG, "Got ${allVitals.size} vitals in range")

                val filteredVitals = allVitals
                    .filter { it.masterVitalId == vitalTypeId }
                    .sortedBy { it.recordedAt }

                Log.d(TAG, "After filtering by vitalTypeId: ${filteredVitals.size} vitals")

                if (filteredVitals.isEmpty()) {
                    // ✅ Clear previous data and update UI
                    _vitalsData.value = emptyList()
                    _stats.value = null
                    _state.value = States.Success(Unit) // Changed from Error to Success
                    Log.w(TAG, "No vitals found for the selected period")
                    return@launch
                }

                // Calculate stats
                val values = filteredVitals.map { it.value }
                val stats = VitalStats(
                    min = values.minOrNull() ?: 0.0,
                    max = values.maxOrNull() ?: 0.0,
                    avg = if (values.isNotEmpty()) values.average() else 0.0
                )

                Log.d(TAG, "Stats calculated: min=${stats.min}, max=${stats.max}, avg=${stats.avg}")

                _vitalsData.value = filteredVitals
                _stats.value = stats
                _state.value = States.Success(Unit)

                Log.d(TAG, "Successfully loaded ${filteredVitals.size} vitals")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading vitals data: ${e.message}", e)
                _state.value = States.Error("Failed to load vitals: ${e.message}")
            }
        }
    }

    fun clearError() {
        _state.value = States.Idle
    }
}