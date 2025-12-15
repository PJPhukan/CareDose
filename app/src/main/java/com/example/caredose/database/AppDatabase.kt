package com.example.caredose.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.caredose.database.dao.*
import com.example.caredose.database.entities.*

@Database(
    entities = [
        User::class,
        Patient::class,
        MasterMedicine::class,
        MasterVital::class,
        MedicineStock::class,
        Dose::class,
        DoseLog::class,
        Vital::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun patientDao(): PatientDao
    abstract fun masterMedicineDao(): MasterMedicineDao
    abstract fun masterVitalDao(): MasterVitalDao
    abstract fun medicineStockDao(): MedicineStockDao
    abstract fun doseDao(): DoseDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun vitalDao(): VitalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "caredose_database"
                )
                    .fallbackToDestructiveMigration()  // ← This destroys old data during dev
                    // For production, add proper migrations
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}