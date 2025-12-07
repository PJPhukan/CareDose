package com.example.caredose.ui.patient.dialogs

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Dose
import com.example.caredose.database.entities.MedicineStock
import com.example.caredose.databinding.DialogAddEditDoseBinding
import com.example.caredose.repository.MedicineStockRepository
import com.example.caredose.viewmodels.MedicineStockViewModel
import com.example.caredose.viewmodels.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.util.*

class AddEditDoseDialog : DialogFragment() {

    private var _binding: DialogAddEditDoseBinding? = null
    private val binding get() = _binding!!

    private var patientId: Long = 0
    private var existingDose: Dose? = null
    private var onSaveListener: ((Dose) -> Unit)? = null

    private lateinit var medicineStockViewModel: MedicineStockViewModel
    private var medicineStocks = listOf<MedicineStock>()
    private var selectedTimeInMinutes: Int = 540 // Default 9:00 AM

    companion object {
        private const val ARG_PATIENT_ID = "patient_id"
        private const val ARG_DOSE = "dose"

        fun newInstance(patientId: Long, dose: Dose? = null): AddEditDoseDialog {
            return AddEditDoseDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PATIENT_ID, patientId)
                    dose?.let { putParcelable(ARG_DOSE, it) }
                }
            }
        }
    }

    fun setOnSaveListener(listener: (Dose) -> Unit) {
        onSaveListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        patientId = arguments?.getLong(ARG_PATIENT_ID) ?: 0
        existingDose = arguments?.getSerializable(ARG_DOSE) as? Dose
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddEditDoseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupUI()
        setupClickListeners()
        observeMedicines()
    }

    private fun setupViewModel() {
        val db = AppDatabase.getDatabase(requireContext())
        val factory = ViewModelFactory(
            medicineStockRepository = MedicineStockRepository(db.medicineStockDao())
        )

        medicineStockViewModel = ViewModelProvider(this, factory)[MedicineStockViewModel::class.java]
        medicineStockViewModel.setPatientId(patientId)
    }

    private fun setupUI() {
        existingDose?.let { dose ->
            binding.tvTitle.text = "Edit Dose Schedule"
            selectedTimeInMinutes = dose.timeInMinutes
            binding.btnSelectTime.text = formatTime(dose.timeInMinutes)
            binding.etQuantity.setText(dose.quantity.toString())
            binding.switchActive.isChecked = dose.isActive
            binding.btnSave.text = "Update"
        } ?: run {
            binding.tvTitle.text = "Add Dose Schedule"
            binding.btnSelectTime.text = formatTime(selectedTimeInMinutes)
            binding.btnSave.text = "Save"
        }
    }

    private fun observeMedicines() {
        medicineStockViewModel.medicineStocks.observe(viewLifecycleOwner) { stocks ->
            medicineStocks = stocks
            setupMedicineSpinner(stocks)
        }
    }

    private fun setupMedicineSpinner(stocks: List<MedicineStock>) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val medicineNames = stocks.map { stock ->
                val medicine = db.masterMedicineDao().getById(stock.masterMedicineId)
                medicine?.name ?: "Unknown"
            }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                medicineNames
            )

            binding.actvMedicine.setAdapter(adapter)

            // Pre-select for edit mode
            existingDose?.let { dose ->
                val selectedStock = stocks.find { it.stockId == dose.stockId }
                selectedStock?.let { stock ->
                    val medicine = db.masterMedicineDao().getById(stock.masterMedicineId)
                    binding.actvMedicine.setText(medicine?.name ?: "", false)
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectTime.setOnClickListener {
            showTimePicker()
        }

        binding.btnSave.setOnClickListener {
            saveDose()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun showTimePicker() {
        val hour = selectedTimeInMinutes / 60
        val minute = selectedTimeInMinutes % 60

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                selectedTimeInMinutes = selectedHour * 60 + selectedMinute
                binding.btnSelectTime.text = formatTime(selectedTimeInMinutes)
            },
            hour,
            minute,
            false // 12-hour format
        ).show()
    }

    private fun formatTime(timeInMinutes: Int): String {
        val hour = timeInMinutes / 60
        val minute = timeInMinutes % 60
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }

        return String.format(
            "%02d:%02d %s",
            if (hour % 12 == 0) 12 else hour % 12,
            minute,
            if (hour < 12) "AM" else "PM"
        )
    }

    private fun saveDose() {
        val medicineName = binding.actvMedicine.text.toString().trim()
        val quantityStr = binding.etQuantity.text.toString()

        // Validation
        if (medicineName.isEmpty()) {
            binding.actvMedicine.error = "Select a medicine"
            return
        }

        if (quantityStr.isEmpty()) {
            binding.etQuantity.error = "Quantity required"
            return
        }

        val quantity = quantityStr.toIntOrNull() ?: 1

        // Find medicine stock ID
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val selectedStock = medicineStocks.find { stock ->
                val medicine = db.masterMedicineDao().getById(stock.masterMedicineId)
                medicine?.name.equals(medicineName, ignoreCase = true)
            }

            if (selectedStock == null) {
                binding.actvMedicine.error = "Medicine not found"
                return@launch
            }

            val dose = existingDose?.copy(
                stockId = selectedStock.stockId,
                timeInMinutes = selectedTimeInMinutes,
                quantity = quantity,
                isActive = binding.switchActive.isChecked
            ) ?: Dose(
                stockId = selectedStock.stockId,
                patientId = patientId,
                timeInMinutes = selectedTimeInMinutes,
                quantity = quantity,
                isActive = binding.switchActive.isChecked
            )

            onSaveListener?.invoke(dose)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}