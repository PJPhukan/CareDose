package com.example.caredose

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.caredose.alarm.MidnightRescheduleScheduler
import com.example.caredose.alarm.StockReminderScheduler
import com.example.caredose.database.AppDatabase
import com.example.caredose.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var database: AppDatabase
    private var hasAskedForNotificationPermission = false
    private var hasAskedForAlarmPermission = false

    companion object {
        private const val TAG = "MainActivity"
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()

            checkExactAlarmPermission()
        } else {
            Toast.makeText(
                this,
                "Notifications disabled. You won't receive dose reminders.",
                Toast.LENGTH_LONG
            ).show()
            checkExactAlarmPermission()
        }
    }

    private val exactAlarmSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (canScheduleExactAlarms()) {
            Toast.makeText(this, "Exact alarm permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Exact alarms not enabled. Reminders may not work at exact times.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        database = AppDatabase.getDatabase(this)

        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val midnightScheduler = MidnightRescheduleScheduler(this)
        val stockReminderScheduler = StockReminderScheduler(this)
        if (!stockReminderScheduler.areStockRemindersScheduled()) {
            stockReminderScheduler.scheduleStockReminders()
        }

        if (!midnightScheduler.isMidnightRescheduleScheduled()) {
            midnightScheduler.scheduleMidnightReschedule()
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_analytics,
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        loadUserName()

        requestAllPermissions()
        handleStockReminderIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleStockReminderIntent(intent)
    }
    private fun handleStockReminderIntent(intent: Intent?) {
        val openStockScreen = intent?.getBooleanExtra("open_stock_screen", false) ?: false

        if (openStockScreen) {
            // TODO: Navigate to your medicine fragment

        }
    }
    private fun loadUserName() {
        val userId = sessionManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                val user = database.userDao().getById(userId)
                supportActionBar?.title = "Welcome, ${user?.name ?: "User"}"
            } catch (e: Exception) {
                supportActionBar?.title = "CareDose"
            }
        }
    }
    private fun requestAllPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission()
        } else {
            checkExactAlarmPermission()
        }
    }


    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

            when {
                isGranted -> {
                    checkExactAlarmPermission()
                }
                shouldShowRequestPermissionRationale(permission) -> {
                    showNotificationPermissionRationale()
                }
                else -> {
                    notificationPermissionLauncher.launch(permission)
                    hasAskedForNotificationPermission = true
                }
            }
        }
    }

    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Enable Notifications")
            .setMessage("CareDose needs notification permission to remind you when it's time to take medicine.")
            .setPositiveButton("Allow") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
                checkExactAlarmPermission()
            }
            .setCancelable(false)
            .show()
    }

    private fun checkExactAlarmPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = canScheduleExactAlarms()
            if (!canSchedule && !hasAskedForAlarmPermission) {
                showExactAlarmPermissionDialog()
            }
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val result = alarmManager.canScheduleExactAlarms()
            result
        } else {
            true
        }
    }

    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable Dose Reminders")
            .setMessage("CareDose needs permission to schedule exact alarms to remind you at the right time to take medicine.\n\nPlease enable 'Alarms & reminders' in the next screen.")
            .setPositiveButton("Open Settings") { _, _ ->
                openExactAlarmSettings()
                hasAskedForAlarmPermission = true
            }
            .setNegativeButton("Later") { dialog, _ ->
                dialog.dismiss()
                hasAskedForAlarmPermission = true
            }
            .setCancelable(false)
            .show()
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                exactAlarmSettingsLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Could not open settings. Please enable manually in Settings > Apps > CareDose",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

  override fun onResume() {
        super.onResume()
        if (hasAskedForAlarmPermission) {
            val canSchedule = canScheduleExactAlarms()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                sessionManager.logout()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}