package com.example.caredose

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.caredose.database.AppDatabase
import com.example.caredose.databinding.ActivityPatientDetailBinding
import com.example.caredose.repository.*
import com.example.caredose.ui.patient.PatientDetailPagerAdapter
import com.example.caredose.viewmodels.*
import com.google.android.material.tabs.TabLayoutMediator

class PatientDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPatientDetailBinding
    private var patientId: Long = 0
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

        setupToolbar()
        setupViewModels()
        setupViewPager()
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
            doseRepository = DoseRepository(db.doseDao()),
            vitalRepository = VitalRepository(db.vitalDao())
        )

        medicineStockViewModel = ViewModelProvider(this, factory)[MedicineStockViewModel::class.java]
        doseViewModel = ViewModelProvider(this, factory)[DoseViewModel::class.java]
        vitalViewModel = ViewModelProvider(this, factory)[VitalViewModel::class.java]

        // Set patient ID for all ViewModels
        medicineStockViewModel.setPatientId(patientId)
        doseViewModel.setPatientId(patientId)
        vitalViewModel.setPatientId(patientId)
    }

    private fun setupViewPager() {
        val adapter = PatientDetailPagerAdapter(this, patientId)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Medicines"
                1 -> "Dose Schedule"
                2 -> "Vitals"
                else -> ""
            }
        }.attach()
    }
}