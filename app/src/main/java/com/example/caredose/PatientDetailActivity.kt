package com.example.caredose

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.caredose.database.AppDatabase
import com.example.caredose.databinding.ActivityPatientDetailBinding
import com.example.caredose.repository.*
import com.example.caredose.ui.patient.PatientDetailPagerAdapter
import com.example.caredose.viewmodels.*
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding
    private var patientId: Long = 0
    private var userId: Long = 0
    private var patientName: String = ""

    private lateinit var medicineStockViewModel: MedicineStockViewModel
    private lateinit var doseViewModel: DoseViewModel
    private lateinit var vitalViewModel: VitalViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get patient from intent
        patientId = intent.getLongExtra("PATIENT_ID", 0)
        patientName = intent.getStringExtra("PATIENT_NAME") ?: "Patient"

        // Check for notification extras
        val openTab = intent.getIntExtra("open_tab", 0)
        val stockId = intent.getLongExtra("stock_id", -1)

        setupToolbar()

        // Get userId from patientId, then setup ViewModels and ViewPager
        lifecycleScope.launch {
            fetchUserIdAndSetup(openTab, stockId)
        }
    }

    private suspend fun fetchUserIdAndSetup(openTab: Int, stockId: Long) {
        val db = AppDatabase.getDatabase(this)
        val patient = db.patientDao().getById(patientId)
        userId = patient?.userId ?: 0

        // Setup on main thread
        runOnUiThread {
            setupViewModels()
            setupViewPager()

            // Open specific tab if requested (from notification)
            if (openTab > 0) {
                binding.viewPager.setCurrentItem(openTab, false)

                // If stock_id is provided, store it for highlighting
                if (stockId != -1L) {
                    getSharedPreferences("notification_prefs", MODE_PRIVATE)
                        .edit()
                        .putLong("highlight_stock_id", stockId)
                        .apply()
                }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = patientName
            setDisplayHomeAsUpEnabled(true)
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViewModels() {
        val db = AppDatabase.getDatabase(this)

        val factory = ViewModelFactory(
            medicineStockRepository = MedicineStockRepository(db.medicineStockDao()),
            doseRepository = DoseRepository(db.doseDao(), db.medicineStockDao(), db.doseLogDao()),
            vitalRepository = VitalRepository(db.vitalDao())
        )

        medicineStockViewModel =
            ViewModelProvider(this, factory)[MedicineStockViewModel::class.java]
        doseViewModel = ViewModelProvider(this, factory)[DoseViewModel::class.java]
        vitalViewModel = ViewModelProvider(this, factory)[VitalViewModel::class.java]


        medicineStockViewModel.setUserId(userId)
        doseViewModel.setPatientId(patientId)
        vitalViewModel.setPatientId(patientId)
    }

    private fun setupViewPager() {
        val adapter = PatientDetailPagerAdapter(this, patientId)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Dose Schedule"
                1 -> "Medicines"
                2 -> "Vitals"
                else -> ""
            }
        }.attach()
    }
}