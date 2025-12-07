package com.example.caredose.ui.patient.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caredose.adapter.VitalAdapter
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Vital
import com.example.caredose.databinding.FragmentVitalsBinding
import com.example.caredose.repository.VitalRepository
import com.example.caredose.ui.patient.dialogs.AddEditVitalDialog
import com.example.caredose.viewmodels.VitalViewModel
import com.example.caredose.viewmodels.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class VitalsFragment : Fragment() {

    private var _binding: FragmentVitalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: VitalViewModel
    private lateinit var adapter: VitalAdapter
    private var patientId: Long = 0

    companion object {
        private const val ARG_PATIENT_ID = "patient_id"

        fun newInstance(patientId: Long): VitalsFragment {
            return VitalsFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PATIENT_ID, patientId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        patientId = arguments?.getLong(ARG_PATIENT_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVitalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupFab()
        observeData()
    }

    private fun setupViewModel() {
        val db = AppDatabase.getDatabase(requireContext())
        val factory = ViewModelFactory(
            vitalRepository = VitalRepository(db.vitalDao())
        )

        viewModel = ViewModelProvider(this, factory)[VitalViewModel::class.java]
        viewModel.setPatientId(patientId)
    }

    private fun setupRecyclerView() {
        adapter = VitalAdapter(
            onEditClick = { vital ->
                showAddEditDialog(vital)
            },
            onDeleteClick = { vital ->
                showDeleteConfirmation(vital)
            }
        )

        binding.rvVitals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@VitalsFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabAddVital.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun observeData() {
        viewModel.vitals.observe(viewLifecycleOwner) { vitals ->
            adapter.submitList(vitals)

            if (vitals.isEmpty()) {
                binding.rvVitals.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvVitals.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
            }
        }
    }

    private fun showAddEditDialog(existingVital: Vital?) {
        val dialog = AddEditVitalDialog.newInstance(patientId, existingVital)

        dialog.setOnSaveListener { vital ->
            if (existingVital == null) {
                viewModel.addVital(vital)
            } else {
                viewModel.updateVital(vital)
            }
        }

        dialog.show(childFragmentManager, "AddEditVitalDialog")
    }

    private fun showDeleteConfirmation(vital: Vital) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Vital Reading")
            .setMessage("Are you sure you want to delete this vital reading?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteVital(vital)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}