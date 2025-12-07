package com.example.caredose.database.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "medicine_stock",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["patientId"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MasterMedicine::class,
            parentColumns = ["medicineId"],
            childColumns = ["masterMedicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patientId"), Index("masterMedicineId")]
)
@Parcelize
data class MedicineStock(
    @PrimaryKey(autoGenerate = true)
    val stockId: Long = 0,
    val patientId: Long,
    val masterMedicineId: Long,
    var stockQty: Int,
    val reminderStockThreshold: Int = 5,
    val isReminderEnabled: Boolean = true,
    val duration: Int,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable
