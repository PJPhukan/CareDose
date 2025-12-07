package com.example.caredose.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "master_medicines",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index(value = ["userId", "name"], unique = true)]
)
data class MasterMedicine(
    val userId: Long,       // Each user has their own list
    val name: String,
    @PrimaryKey(autoGenerate = true)
    val medicineId: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
