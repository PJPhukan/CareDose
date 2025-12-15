package com.example.caredose

import android.app.Application
import android.util.Log
import com.example.caredose.alarm.MidnightRescheduleScheduler
import com.example.caredose.alarm.StockReminderScheduler

class CareDoseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("CareDoseApplication", "🚀 App started - Initializing midnight scheduler")

        // Schedule the midnight reschedule alarm
        val midnightScheduler = MidnightRescheduleScheduler(this)
        val stockReminderScheduler = StockReminderScheduler(this)
        if (!stockReminderScheduler.areStockRemindersScheduled()) {
            stockReminderScheduler.scheduleStockReminders()
            Log.d("CareDoseApplication", "📅 Stock reminders initialized for the first time")
        } else {
            Log.d("CareDoseApplication", "✅ Stock reminders already scheduled, skipping")
        }

        // Check if already scheduled to avoid duplicate
        if (!midnightScheduler.isMidnightRescheduleScheduled()) {
            midnightScheduler.scheduleMidnightReschedule()
            Log.d("CareDoseApplication", "✅ Midnight scheduler initialized")
        } else {
            Log.d("CareDoseApplication", "ℹ️ Midnight scheduler already running")
        }
    }
}