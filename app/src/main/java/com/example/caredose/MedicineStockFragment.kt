package com.example.caredose.ui.patient.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caredose.adapter.MedicineStockAdapter
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MedicineStock
import com.example.caredose.databinding.FragmentMedicineStockBinding
import com.example.caredose.repository.MedicineStockRepository
import com.example.caredose.ui.patient.dialogs.AddEditMedicineStockDialog
import com.example.caredose.ui.patient.dialogs.QuickAddStockDialog
import com.example.caredose.viewmodels.MedicineStockViewModel
import com.example.caredose.viewmodels.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MedicineStockFragment : Fragment() {

    private var _binding: FragmentMedicineStockBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MedicineStockViewModel
    private lateinit var adapter: MedicineStockAdapter
    private var patientId: Long = 0
    private var userId: Long = 0
    private var isViewModelInitialized = false

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

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val patient = db.patientDao().getById(patientId)
            userId = patient?.userId ?: 0

            setupViewModel()

            if (_binding != null) {
                observeData()
            }
        }
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

        setupRecyclerView()
        setupFab()

        if (isViewModelInitialized) {
            observeData()
        }

        checkForHighlightRequest()
    }

    private fun setupViewModel() {
        val db = AppDatabase.getDatabase(requireContext())
        val factory = ViewModelFactory(
            medicineStockRepository = MedicineStockRepository(db.medicineStockDao())
        )

        viewModel = ViewModelProvider(this, factory)[MedicineStockViewModel::class.java]
        viewModel.setUserId(userId)
        isViewModelInitialized = true
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
                showQuickAddStockDialog(stock)
            },
            onDecrementStock = { stock ->
                if (isViewModelInitialized && stock.stockQty > 0) {
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
        if (!isViewModelInitialized) return

        viewModel.medicineStocks.observe(viewLifecycleOwner) { stocks ->
            adapter.submitList(stocks)

            if (stocks.isEmpty()) {
                binding.rvMedicineStock.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvMedicineStock.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE

                scrollToHighlightedItem(stocks)
            }
        }
    }

    private fun checkForHighlightRequest() {
       val prefs = requireActivity().getSharedPreferences("notification_prefs", android.content.Context.MODE_PRIVATE)
        val highlightStockId = prefs.getLong("highlight_stock_id", -1)

        if (highlightStockId != -1L) {
            // Clear the preference
            prefs.edit().remove("highlight_stock_id").apply()

            // Show a message to add stock
            com.google.android.material.snackbar.Snackbar.make(
                binding.root,
                "⚠️ Please add stock for this medicine",
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
            ).show()
        }
    }

    private fun scrollToHighlightedItem(stocks: List<MedicineStock>) {
        val prefs = requireActivity().getSharedPreferences("notification_prefs", android.content.Context.MODE_PRIVATE)
        val highlightStockId = prefs.getLong("highlight_stock_id", -1)

        if (highlightStockId != -1L) {
            val position = stocks.indexOfFirst { it.stockId == highlightStockId }
            if (position != -1) {
                binding.rvMedicineStock.scrollToPosition(position)

            binding.rvMedicineStock.postDelayed({
                    showAddEditDialog(stocks[position])
                }, 300)
            }
        }
    }

    private fun showQuickAddStockDialog(stock: MedicineStock) {
        lifecycleScope.launch {
            // Get medicine name
            val db = AppDatabase.getDatabase(requireContext())
            val medicine = db.masterMedicineDao().getById(stock.masterMedicineId)
            val medicineName = medicine?.name ?: "Medicine"

            val dialog = QuickAddStockDialog.newInstance(stock, medicineName)

            dialog.setOnStockUpdatedListener { quantity ->
                // Stock is already updated in database by the dialog
                // LiveData will automatically refresh the list
            }

            dialog.show(childFragmentManager, "QuickAddStockDialog")
        }
    }

    private fun showAddEditDialog(existingStock: MedicineStock?) {
        if (!isViewModelInitialized) return

        val dialog = AddEditMedicineStockDialog.newInstance(userId, existingStock)

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
        if (!isViewModelInitialized) return

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