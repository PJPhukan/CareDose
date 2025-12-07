package com.example.caredose.database

import com.example.caredose.database.entities.MasterMedicine
import com.example.caredose.database.entities.MasterVital

object PrePopulateHelper {
    suspend fun populateDefaultMedicines(database: AppDatabase, userId: Long) {
        val commonMedicines = listOf<MasterMedicine>(
            MasterMedicine(userId = userId, name = "Paracetamol"),
            MasterMedicine(userId = userId, name = "Aspirin"),
            MasterMedicine(userId = userId, name = "Ibuprofen"),
            MasterMedicine(userId = userId, name = "Amoxicillin"),
            MasterMedicine(userId = userId, name = "Metformin"),
            MasterMedicine(userId = userId, name = "Omeprazole"),
            MasterMedicine(userId = userId, name = "Atorvastatin"),
            MasterMedicine(userId = userId, name = "Vitamin D"),
            MasterMedicine(userId = userId, name = "Calcium"),
            MasterMedicine(userId = userId, name = "Multivitamin")
        )
        database.masterMedicineDao().insertAll(commonMedicines)
    }


    suspend fun populateDefaultVitals(database: AppDatabase, userId: Long) {
        val commonVitals = listOf<MasterVital>(
            MasterVital(userId = userId, name = "Blood Pressure", unit = "mmHg"),
            MasterVital(userId = userId, name = "Heart Rate", unit = "bpm"),
            MasterVital(userId = userId, name = "SpO2", unit = "%"),
            MasterVital(userId = userId, name = "Temperature", unit = "°F"),
            MasterVital(userId = userId, name = "Blood Sugar", unit = "mg/dL"),
            MasterVital(userId = userId, name = "Weight", unit = "kg"),
            MasterVital(userId = userId, name = "Height", unit = "cm")
        )
        database.masterVitalDao().insertAll(commonVitals)
    }

    suspend fun initializeUserMasterLists(database: AppDatabase, userId: Long) {
        populateDefaultMedicines(database, userId)
        populateDefaultVitals(database, userId)
    }
}