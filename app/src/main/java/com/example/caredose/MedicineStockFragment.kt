package com.example.caredose.ui.patient.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caredose.adapter.MedicineStockAdapter
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MedicineStock
import com.example.caredose.databinding.FragmentMedicineStockBinding
import com.example.caredose.repository.MasterMedicineRepository
import com.example.caredose.repository.MedicineStockRepository
import com.example.caredose.ui.patient.dialogs.AddEditMedicineStockDialog
import com.example.caredose.viewmodels.MedicineStockViewModel
import com.example.caredose.viewmodels.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MedicineStockFragment : Fragment() {

    private var _binding: FragmentMedicineStockBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MedicineStockViewModel
    private lateinit var adapter: MedicineStockAdapter
    private var patientId: Long = 0

    companion object {
        private const val ARG_PATIENT_ID = "patient_id"

        fun newInstance(patientId: Long): MedicineStockFragment {
            return MedicineStockFragment().apply {
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
        _binding = FragmentMedicineStockBinding.inflate(inflater, container, false)
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
            medicineStockRepository = MedicineStockRepository(db.medicineStockDao())
        )

        viewModel = ViewModelProvider(this, factory)[MedicineStockViewModel::class.java]
        viewModel.setPatientId(patientId)
    }

    private fun setupRecyclerView() {
        adapter = MedicineStockAdapter(
            onEditClick = { stock ->
                showAddEditDialog(stock)
            },
            onDeleteClick = { stock ->
                showDeleteConfirmation(stock)
            },
            onIncrementStock = { stock ->
                viewModel.incrementStock(stock.stockId, 1)
            },
            onDecrementStock = { stock ->
                if (stock.stockQty > 0) {
                    viewModel.decrementStock(
                        stock.stockId,
                        1,
                        onSuccess = { /* Stock decremented */ },
                        onFailed = { showStockEmptyMessage() }
                    )
                } else {
                    showStockEmptyMessage()
                }
            }
        )

        binding.rvMedicineStock.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MedicineStockFragment.adapter
        }
    }

    private fun setupFab() {
        binding.fabAddMedicine.setOnClickListener {
            showAddEditDialog(null)
        }
    }

    private fun observeData() {
        viewModel.medicineStocks.observe(viewLifecycleOwner) { stocks ->
            adapter.submitList(stocks)

            if (stocks.isEmpty()) {
                binding.rvMedicineStock.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvMedicineStock.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
            }
        }
    }

    private fun showAddEditDialog(existingStock: MedicineStock?) {
        val dialog = AddEditMedicineStockDialog.newInstance(patientId, existingStock)

        dialog.setOnSaveListener { stock ->
            if (existingStock == null) {
                viewModel.addStock(stock)
            } else {
                viewModel.updateStock(stock)
            }
        }

        dialog.show(childFragmentManager, "AddEditMedicineStockDialog")
    }

    private fun showDeleteConfirmation(stock: MedicineStock) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Medicine Stock")
            .setMessage("Are you sure you want to delete this medicine? This will also delete all associated dose schedules.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteStock(stock)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showStockEmptyMessage() {
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "Stock is empty. Please add stock first.",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}