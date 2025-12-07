package com.example.caredose.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = Dose::class,
            parentColumns = ["doseId"],
            childColumns = ["doseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("doseId")]
)
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0,
    val doseId: Long,
    val takenAt: Long = System.currentTimeMillis(),
    val qtyTaken: Int = 1
)
