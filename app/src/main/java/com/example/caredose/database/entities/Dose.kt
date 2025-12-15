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
    indices = [Index("stockId"), Index("patientId")]
)
@Parcelize
data class Dose(

    val stockId: Long,
    val patientId: Long,
    val medicineId: Long,
    val timeInMinutes: Int,  // Minutes since midnight (e.g., 540 = 9:00 AM)
    val quantity: Int = 1,  // Number of tablets/units per dose
    val isActive: Boolean = true,
    val reminderMinutesBefore : Int,
    val isTakenToday: Boolean = false,
    val lastTakenAt: Long = 0,
    @PrimaryKey(autoGenerate = true)
    val doseId: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
): Parcelable