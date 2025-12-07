package com.example.caredose.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caredose.AddEditPatient

import com.example.caredose.SessionManager
import com.example.caredose.States
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Patient
import com.example.caredose.databinding.FragmentHomeBinding
import com.example.caredose.repository.PatientRepository
import com.example.caredose.PatientDetailActivity

import com.example.caredose.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch
import kotlin.reflect.typeOf

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var patientAdapter: PatientAdapter
    private lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        supportActionBar?.hide()
        // Initialize SessionManager
        sessionManager = SessionManager(requireContext())

        // Setup ViewModel with Global Factory
        val database = AppDatabase.getDatabase(requireContext())
        val patientRepository = PatientRepository(database)
        val factory = ViewModelFactory(patientRepository = patientRepository)
        homeViewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        // Setup RecyclerView
        setupRecyclerView()


        // Setup FAB
        binding.fabAddPatient.setOnClickListener {
            val intent = Intent(requireContext(), AddEditPatient::class.java)
            startActivity(intent)
        }

        // Observe patients
        observePatients()

        // Load patients for current user
        val userId = sessionManager.getUserId()
        if (userId != -1L) {
            homeViewModel.loadPatients(userId as Long)
        }
    }

    private fun setupRecyclerView() {
        patientAdapter = PatientAdapter(
            onPatientClick = { patient ->
                val intent = Intent(requireContext(), PatientDetailActivity::class.java)
                intent.putExtra("PATIENT_ID", patient.patientId)
                intent.putExtra("PATIENT_NAME", patient.name)
                intent.putExtra("PATIENT_AGE", patient.age)
                intent.putExtra("PATIENT_GENDER", patient.gender)
                startActivity(intent)
            },
            onEditClick = { patient ->
                val intent = Intent(requireContext(), AddEditPatient::class.java)
                intent.putExtra("PATIENT_ID", patient.patientId)
                intent.putExtra("PATIENT_NAME", patient.name)
                intent.putExtra("PATIENT_AGE", patient.age)
                intent.putExtra("PATIENT_GENDER", patient.gender)
                startActivity(intent)
            },
            onDeleteClick = { patient ->
                // Delete without confirmation (as requested)
                homeViewModel.deletePatient(patient)
            }

        )

        binding.recyclerViewPatients.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = patientAdapter
        }
    }

    private fun observePatients() {
        viewLifecycleOwner.lifecycleScope.launch {
            homeViewModel.patients.collect { state ->
                when (state) {
                    is States.Idle -> {
                        binding.progressBar.visibility = View.GONE
                    }

                    is States.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyState.visibility = View.GONE
                    }

                    is States.Success -> {
                        binding.progressBar.visibility = View.GONE

                        // 'state.data' is now a List<Patient>
                        val patients: List<Patient> = state.data

                        if (patients.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.recyclerViewPatients.visibility = View.GONE

                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.recyclerViewPatients.visibility = View.VISIBLE
                            patientAdapter.submitList(patients)
                        }
                    }

                    is States.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        // Reload patients when returning from AddEditPatientActivity
        val userId = sessionManager.getUserId()
        userId?.let { id ->
            if (id != -1L) {
                homeViewModel.loadPatients(id)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}