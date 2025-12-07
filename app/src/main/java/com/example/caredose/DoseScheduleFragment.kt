package com.example.caredose.ui.patient.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caredose.adapter.DoseAdapter
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Dose
import com.example.caredose.databinding.FragmentDoseScheduleBinding
import com.example.caredose.repository.DoseRepository
import com.example.caredose.repository.MedicineStockRepository
import com.example.caredose.ui.patient.dialogs.AddEditDoseDialog
import com.example.caredose.viewmodels.DoseViewModel
import com.example.caredose.viewmodels.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DoseScheduleFragment : Fragment() {

    private var _binding: FragmentDoseScheduleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DoseViewModel
    private lateinit var adapter: DoseAdapter
    private var patientId: Long = 0

    companion object {
        private const val ARG_PATIENT_ID = "patient_id"

        fun newInstance(patientId: Long): DoseScheduleFragment {
            return DoseScheduleFragment().apply {
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
        _binding = FragmentDoseScheduleBinding.inflate(inflater, container, false)
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
            doseRepository = DoseRepository(db.doseDao())
        )

        viewModel = ViewModelProvider(this, factory)[DoseViewModel::class.java]
        viewModel.setPatientId(patientId)
    }

    private fun setupRecyclerView() {
        adapter = DoseAdapter(
            onEditClick = { dose ->
                showAddEditDialog(dose)
            },
            onDeleteClick = { dose ->
                showDeleteConfirmation(dose)
            }
        )

        binding.rvDoses.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@DoseScheduleFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabAddDose.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun observeData() {
        viewModel.doses.observe(viewLifecycleOwner) { doses ->
            adapter.submitList(doses)

            if (doses.isEmpty()) {
                binding.rvDoses.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvDoses.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
            }
        }
    }

    private fun showAddEditDialog(existingDose: Dose?) {
        val dialog = AddEditDoseDialog.newInstance(patientId, existingDose)

        dialog.setOnSaveListener { dose ->
            if (existingDose == null) {
                viewModel.addDose(dose)
            } else {
                viewModel.updateDose(dose)
            }
        }

        dialog.show(childFragmentManager, "AddEditDoseDialog")
    }

    private fun showDeleteConfirmation(dose: Dose) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Dose Schedule")
            .setMessage("Are you sure you want to delete this dose schedule?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteDose(dose)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}