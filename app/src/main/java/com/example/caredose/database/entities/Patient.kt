package com.example.caredose.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "patients",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class Patient(
    val userId: Long,
    val name: String,
    val age: Int,
    val gender: String,
    @PrimaryKey(autoGenerate = true)
    val patientId: Long = 0,

    val createdAt: Long = System.currentTimeMillis()
)
