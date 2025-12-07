package com.example.caredose.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "master_vitals",
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
data class MasterVital(
      val userId: Long,
    val name: String,
    val unit: String,
    @PrimaryKey(autoGenerate = true)
    val vitalId: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

