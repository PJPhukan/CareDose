package com.example.caredose.alarm

import com.example.caredose.database.entities.Dose

interface AlarmSchedular {
    fun scheduleReminderDose(dose: Dose,medicineName: String, patientName: String,medicineId:Long)
    fun cancelScheduleReminder(dose: Dose)
    fun cancelReminderByDoseId(doseId: Long)

}