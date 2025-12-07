package com.example.caredose.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.caredose.repository.*
import com.example.caredose.ui.dashboard.DashboardViewModel
import com.example.caredose.ui.home.HomeViewModel
import com.example.caredose.ui.notifications.NotificationsViewModel

class ViewModelFactory(
    private val patientRepository: PatientRepository? = null,
    private val masterMedicineRepository: MasterMedicineRepository? = null,
    private val masterVitalRepository: MasterVitalRepository? = null,
    private val medicineStockRepository: MedicineStockRepository? = null,
    private val doseRepository: DoseRepository? = null,
    private val vitalRepository: VitalRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                requireNotNull(patientRepository) { "PatientRepository required" }
                HomeViewModel(patientRepository) as T
            }

            modelClass.isAssignableFrom(PatientViewModel::class.java) -> {
                requireNotNull(patientRepository) { "PatientRepository required" }
                PatientViewModel(patientRepository) as T
            }

            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel() as T
            }

            modelClass.isAssignableFrom(NotificationsViewModel::class.java) -> {
                NotificationsViewModel() as T
            }

            modelClass.isAssignableFrom(MasterMedicineViewModel::class.java) -> {
                requireNotNull(masterMedicineRepository) { "MasterMedicineRepository required" }
                MasterMedicineViewModel(masterMedicineRepository) as T
            }

            modelClass.isAssignableFrom(MasterVitalViewModel::class.java) -> {
                requireNotNull(masterVitalRepository) { "MasterVitalRepository required" }
                MasterVitalViewModel(masterVitalRepository) as T
            }

            modelClass.isAssignableFrom(MedicineStockViewModel::class.java) -> {
                requireNotNull(medicineStockRepository) { "MedicineStockRepository required" }
                MedicineStockViewModel(medicineStockRepository) as T
            }

            modelClass.isAssignableFrom(DoseViewModel::class.java) -> {
                requireNotNull(doseRepository) { "DoseRepository required" }
                DoseViewModel(doseRepository) as T
            }

            modelClass.isAssignableFrom(VitalViewModel::class.java) -> {
                requireNotNull(vitalRepository) { "VitalRepository required" }
                VitalViewModel(vitalRepository) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}