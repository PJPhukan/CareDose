package com.example.caredose

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.caredose.database.AppDatabase
import com.example.caredose.databinding.ActivityPatientDetailBinding
import com.example.caredose.repository.*
import com.example.caredose.ui.patient.PatientDetailPagerAdapter
import com.example.caredose.viewmodels.*
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding
    private var patientId: Long = -1L
    private var userId: Long = 0
    private var patientName: String = ""

    private lateinit var medicineStockViewModel: MedicineStockViewModel
    private lateinit var doseViewModel: DoseViewModel
    private lateinit var vitalViewModel: VitalViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPatientDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
       patientId = intent.getLongExtra("patient_id", -1L)
        if (patientId == -1L) {
            patientId = intent.getLongExtra("PATIENT_ID", -1L)
        }

        patientName = intent.getStringExtra("PATIENT_NAME") ?: intent.getStringExtra("patient_name") ?: "Patient"

        val openTab = intent.getIntExtra("open_tab", 0)
        val stockId = intent.getLongExtra("stock_id", -1)


        if (patientId != -1L) {
            supportActionBar?.title = patientName

            lifecycleScope.launch {
                fetchUserIdAndSetup(openTab, stockId)
            }
        } else {
            Log.e("PatientDetailActivity", "Error: Patient ID is missing or invalid")
        }
    }

    private suspend fun fetchUserIdAndSetup(openTab: Int, stockId: Long) {
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@PatientDetailActivity)
            val patient = db.patientDao().getById(patientId)

            userId = patient?.userId ?: 0
            if (patientName == "Patient" && patient != null) {
                patientName = patient.name
            }

            withContext(Dispatchers.Main) {
                supportActionBar?.title = patientName
                setupViewModels()
                setupViewPager()

               if (openTab > 0) {
                    binding.viewPager.setCurrentItem(openTab, false)

                 if (stockId != -1L) {
                        getSharedPreferences("notification_prefs", MODE_PRIVATE)
                            .edit()
                            .putLong("highlight_stock_id", stockId)
                            .apply()
                    }
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

        medicineStockViewModel = ViewModelProvider(this, factory)[MedicineStockViewModel::class.java]
        doseViewModel = ViewModelProvider(this, factory)[DoseViewModel::class.java]
        vitalViewModel = ViewModelProvider(this, factory)[VitalViewModel::class.java]

        medicineStockViewModel.setUserId(userId)
        doseViewModel.setPatientId(patientId)
        vitalViewModel.setPatientId(patientId)
    }

    private fun setupViewPager() {val adapter = PatientDetailPagerAdapter(this, patientId)
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