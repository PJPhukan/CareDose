package com.example.caredose.database.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "doses",
    foreignKeys = [
        ForeignKey(
            entity = MedicineStock::class,
            parentColumns = ["stockId"],
            childColumns = ["stockId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["patientId"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stockId"), Index("patientId"), Index("scheduleGroupId")]
)
@Parcelize
data class Dose(
    @PrimaryKey(autoGenerate = true)
    val doseId: Long = 0,
    val scheduleGroupId: String,
    val stockId: Long,
    val patientId: Long,
    val medicineId: Long,
    val timeInMinutes: Int,
    val quantity: Int = 1,
    val durationType: String,
    val durationValue: Int?,
    val startDate: Long,
    val endDate: Long?,
    val reminderMinutesBefore: Int,
    val isActive: Boolean = true,
    val isTakenToday: Boolean = false,
    val lastTakenAt: Long = 0,

    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {
    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
        return endDate != null && endDate < currentTime
    }

    fun isValidSchedule(currentTime: Long = System.currentTimeMillis()): Boolean {
        return isActive && !isExpired(currentTime)
    }

    fun getDurationTypeEnum(): DurationType {
        return DurationType.fromString(durationType)
    }
}