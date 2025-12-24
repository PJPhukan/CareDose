package com.example.caredose
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Patient
import com.example.caredose.databinding.ActivityAddEditPatientBinding
import com.example.caredose.repository.PatientRepository

import com.example.caredose.viewmodels.PatientViewModel
import com.example.caredose.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch
class AddEditPatient : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditPatientBinding
    private lateinit var patientViewModel: PatientViewModel
    private lateinit var sessionManager: SessionManager

    private var patientId: Long = -1L
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SessionManager
        sessionManager = SessionManager(this)

        val database = AppDatabase.getDatabase(this)
        val patientRepository = PatientRepository(database)
        val factory = ViewModelFactory(patientRepository = patientRepository)
        patientViewModel = ViewModelProvider(this, factory)[PatientViewModel::class.java]

        setupGenderSpinner()

        checkEditMode()

        binding.btnSave.setOnClickListener {
            savePatient()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        observeOperationState()
    }

    private fun setupGenderSpinner() {
        val genders = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = adapter
    }

    private fun checkEditMode() {
        patientId = intent.getLongExtra("PATIENT_ID", -1L)

        if (patientId != -1L) {
            isEditMode = true
            binding.tvTitle.text = "Edit Patient"

            val name = intent.getStringExtra("PATIENT_NAME") ?: ""
            val age = intent.getIntExtra("PATIENT_AGE", 0)
            val gender = intent.getStringExtra("PATIENT_GENDER") ?: "Male"

            binding.etPatientName.setText(name)
            binding.etPatientAge.setText(age.toString())

            val genderPosition = when (gender) {
                "Male" -> 0
                "Female" -> 1
                "Other" -> 2
                else -> 0
            }
            binding.spinnerGender.setSelection(genderPosition)
        } else {
            isEditMode = false
            binding.tvTitle.text = "Add Patient"
        }
    }

    private fun savePatient() {
        // Get input values
        val name = binding.etPatientName.text.toString().trim()
        val ageStr = binding.etPatientAge.text.toString().trim()
        val gender = binding.spinnerGender.selectedItem.toString()

        // Validate
        if (name.isEmpty()) {
            binding.etPatientName.error = "Name is required"
            binding.etPatientName.requestFocus()
            return
        }

        if (ageStr.isEmpty()) {
            binding.etPatientAge.error = "Age is required"
            binding.etPatientAge.requestFocus()
            return
        }

        val age = ageStr.toIntOrNull()
        if (age == null || age <= 0 || age > 150) {
            binding.etPatientAge.error = "Enter valid age (1-150)"
            binding.etPatientAge.requestFocus()
            return
        }

        val userId = sessionManager.getUserId()
        if (userId == -1L) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Create/Update Patient
        val patient = Patient(
            patientId = if (isEditMode) patientId else 0,
            userId = userId as Long,
            name = name,
            age = age,
            gender = gender
        )

        if (isEditMode) {
            patientViewModel.updatePatient(patient)
        } else {
            patientViewModel.addPatient(patient)
        }
    }

    private fun observeOperationState() {
        lifecycleScope.launch {
            patientViewModel.operationState.collect { state ->
                when (state) {
                    is States.Idle -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    is States.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnSave.isEnabled = false
                    }
                    is States.Success -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        val message = if (isEditMode) "Patient updated" else "Patient added"
                        Toast.makeText(this@AddEditPatient, message, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is States.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(this@AddEditPatient, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}