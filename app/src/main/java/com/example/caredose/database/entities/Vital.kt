package com.example.caredose.database.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize


@Entity(
    tableName = "vitals",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["patientId"],
            childColumns = ["patientId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MasterVital::class,
            parentColumns = ["vitalId"],
            childColumns = ["masterVitalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patientId"), Index("masterVitalId")]
)
@Parcelize
data class Vital(
    @PrimaryKey(autoGenerate = true)
    val vitalRecordId: Long = 0,
    val patientId: Long,
    val masterVitalId: Long,
    val value: Double,
    val recordedAt: Long = System.currentTimeMillis(),
    val note: String? = null
): Parcelable
