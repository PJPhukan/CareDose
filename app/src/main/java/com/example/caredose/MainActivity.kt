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

    // ✅ Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Log.d(TAG, "Notification permission result: $isGranted")
        if (isGranted) {
            Toast.makeText(this, "✓ Notification permission granted", Toast.LENGTH_SHORT).show()
            // After notification permission, check exact alarm
            checkExactAlarmPermission()
        } else {
            Toast.makeText(
                this,
                "Notifications disabled. You won't receive dose reminders.",
                Toast.LENGTH_LONG
            ).show()
            // Still check exact alarm even if notification was denied
            checkExactAlarmPermission()
        }
    }

    // ✅ Activity result for exact alarm settings
    private val exactAlarmSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d(TAG, "Returned from exact alarm settings")
        if (canScheduleExactAlarms()) {
            Toast.makeText(this, "✓ Exact alarm permission granted", Toast.LENGTH_SHORT).show()
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
        Log.d(TAG, "onCreate started")

        sessionManager = SessionManager(this)
        database = AppDatabase.getDatabase(this)

        // Check if logged in
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
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

        // Load and display user name
        loadUserName()

        // ✅ Request permissions on first launch
        Log.d(TAG, "Requesting permissions...")
        requestAllPermissions()
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

    // ✅ Request all necessary permissions
    private fun requestAllPermissions() {
        Log.d(TAG, "Android Version: ${Build.VERSION.SDK_INT}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Android 13+, checking notification permission...")
            checkNotificationPermission()
        } else {
            Log.d(TAG, "Android 12 or lower, skipping notification permission")
            checkExactAlarmPermission()
        }
    }

    // ✅ Check and request notification permission (Android 13+)
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "Notification permission granted: $isGranted")

            when {
                isGranted -> {
                    Log.d(TAG, "Notification permission already granted")
                    checkExactAlarmPermission()
                }
                shouldShowRequestPermissionRationale(permission) -> {
                    Log.d(TAG, "Showing notification permission rationale")
                    showNotificationPermissionRationale()
                }
                else -> {
                    Log.d(TAG, "Requesting notification permission")
                    notificationPermissionLauncher.launch(permission)
                    hasAskedForNotificationPermission = true
                }
            }
        }
    }

    // ✅ Show rationale for notification permission
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

    // ✅ Check and request exact alarm permission (Android 12+)
    private fun checkExactAlarmPermission() {
        Log.d(TAG, "checkExactAlarmPermission called")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = canScheduleExactAlarms()
            Log.d(TAG, "Can schedule exact alarms: $canSchedule")
            Log.d(TAG, "Already asked for alarm permission: $hasAskedForAlarmPermission")

            if (!canSchedule && !hasAskedForAlarmPermission) {
                Log.d(TAG, "Showing exact alarm permission dialog")
                showExactAlarmPermissionDialog()
            } else if (canSchedule) {
                Log.d(TAG, "Exact alarm permission already granted")
            } else {
                Log.d(TAG, "User already denied alarm permission")
            }
        } else {
            Log.d(TAG, "Android 11 or lower, exact alarm permission not needed")
        }
    }

    // ✅ Check if app can schedule exact alarms
    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val result = alarmManager.canScheduleExactAlarms()
            Log.d(TAG, "canScheduleExactAlarms result: $result")
            result
        } else {
            Log.d(TAG, "canScheduleExactAlarms: true (Android < 12)")
            true
        }
    }

    // ✅ Show dialog to enable exact alarm permission
    private fun showExactAlarmPermissionDialog() {
        Log.d(TAG, "Showing exact alarm dialog")
        AlertDialog.Builder(this)
            .setTitle("Enable Dose Reminders")
            .setMessage("CareDose needs permission to schedule exact alarms to remind you at the right time to take medicine.\n\nPlease enable 'Alarms & reminders' in the next screen.")
            .setPositiveButton("Open Settings") { _, _ ->
                Log.d(TAG, "User clicked Open Settings")
                openExactAlarmSettings()
                hasAskedForAlarmPermission = true
            }
            .setNegativeButton("Later") { dialog, _ ->
                Log.d(TAG, "User clicked Later")
                dialog.dismiss()
                hasAskedForAlarmPermission = true
            }
            .setCancelable(false)
            .show()
    }

    // ✅ Open exact alarm settings
    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                Log.d(TAG, "Opening exact alarm settings")
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                exactAlarmSettingsLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error opening settings: ${e.message}")
                Toast.makeText(
                    this,
                    "Could not open settings. Please enable manually in Settings > Apps > CareDose",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ✅ Check if permissions were granted when returning from settings
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")

        if (hasAskedForAlarmPermission) {
            val canSchedule = canScheduleExactAlarms()
            Log.d(TAG, "onResume - Can schedule exact alarms: $canSchedule")
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