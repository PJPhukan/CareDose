package com.example.caredose

import android.app.Application
import android.util.Log
import com.example.caredose.alarm.MidnightRescheduleScheduler
import com.example.caredose.alarm.StockReminderScheduler
import com.example.caredose.workers.DailyCleanupScheduler

class CareDoseApplication : Application() {

    override fun onCreate() {
        super.onCreate()


        DailyCleanupScheduler.schedule(this)
    }
}