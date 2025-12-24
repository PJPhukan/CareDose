package com.example.caredose.ui.patient.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MedicineStock
import com.example.caredose.databinding.DialogQuickAddStockBinding
import kotlinx.coroutines.launch

class QuickAddStockDialog : DialogFragment() {

    private var _binding: DialogQuickAddStockBinding? = null
    private val binding get() = _binding!!

    private var stock: MedicineStock? = null
    private var medicineName: String = ""
    private var onStockUpdated: ((Int) -> Unit)? = null

    companion object {
        private const val ARG_STOCK = "stock"
        private const val ARG_MEDICINE_NAME = "medicine_name"

        fun newInstance(
            stock: MedicineStock,
            medicineName: String
        ): QuickAddStockDialog {
            return QuickAddStockDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_STOCK, stock)
                    putString(ARG_MEDICINE_NAME, medicineName)
                }
            }
        }
    }

    fun setOnStockUpdatedListener(listener: (Int) -> Unit) {
        onStockUpdated = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        stock = arguments?.getParcelable(ARG_STOCK)
        medicineName = arguments?.getString(ARG_MEDICINE_NAME) ?: "Medicine"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogQuickAddStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        stock?.let { currentStock ->
            binding.tvMedicineName.text = medicineName
            binding.tvCurrentStock.text = "Current Stock: ${currentStock.stockQty} units"

            // Prefill the quantity field with current stock amount
            binding.etQuantity.setText(currentStock.stockQty.toString())

            // Show quick add buttons
            setupQuickAddButtons()
        }
    }

    private fun setupQuickAddButtons() {
        binding.btnAdd10.setOnClickListener { incrementQuantity(10) }
        binding.btnAdd20.setOnClickListener { incrementQuantity(20) }
        binding.btnAdd50.setOnClickListener { incrementQuantity(50) }
    }

    private fun incrementQuantity(amount: Int) {
        val currentQuantityStr = binding.etQuantity.text.toString().trim()
        val currentQuantity = currentQuantityStr.toIntOrNull() ?: 0
        val newQuantity = currentQuantity + amount
        binding.etQuantity.setText(newQuantity.toString())
    }

    private fun setupClickListeners() {
        binding.btnAddCustom.setOnClickListener {
            val quantityStr = binding.etQuantity.text.toString().trim()

            if (quantityStr.isEmpty()) {
                binding.etQuantity.error = "Enter quantity"
                return@setOnClickListener
            }

            val newQuantity = quantityStr.toIntOrNull()
            if (newQuantity == null || newQuantity <= 0) {
                binding.etQuantity.error = "Enter valid quantity"
                return@setOnClickListener
            }

            updateStock(newQuantity)
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updateStock(newQuantity: Int) {
        stock?.let { currentStock ->
            lifecycleScope.launch {
                try {
                    val db = AppDatabase.getDatabase(requireContext())

                    val quantityAdded = newQuantity - currentStock.stockQty

                    // Update stock in database
                    val updatedStock = currentStock.copy(
                        stockQty = newQuantity
                    )
                    db.medicineStockDao().update(updatedStock)

                    // Notify listener with the amount added
                    onStockUpdated?.invoke(quantityAdded)

                    Toast.makeText(
                        requireContext(),
                        "Stock updated to $newQuantity units (${if (quantityAdded >= 0) "+" else ""}$quantityAdded)",
                        Toast.LENGTH_SHORT
                    ).show()

                    dismiss()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to update stock",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}