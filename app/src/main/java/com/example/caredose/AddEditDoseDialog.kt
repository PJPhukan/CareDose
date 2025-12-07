package com.example.caredose.ui.patient.dialogs

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
import kotlinx.coroutines.launch
import java.util.*

// Helper data class to hold the necessary medicine information
private data class MedicineInfo(
    val stockId: Long,
    val name: String
)

class AddEditDoseDialog : DialogFragment() {

    private var _binding: DialogAddEditDoseBinding? = null
    private val binding get() = _binding!!

    private var patientId: Long = 0
    private var existingDose: Dose? = null
    private var onSaveListener: ((Dose) -> Unit)? = null

    private lateinit var medicineStockViewModel: MedicineStockViewModel
    private var medicineStocks = listOf<MedicineStock>()
    private var medicineInfoList = listOf<MedicineInfo>() // Stores StockId and Name
    private var selectedTimeInMinutes: Int = 540 // Default 9:00 AM
    private var reminderMinutesBefore: Int = 15 // Default 15 minutes

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
        _binding = DialogAddEditDoseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set dialog width to 90% of screen
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

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
            reminderMinutesBefore = dose.reminderMinutesBefore ?: 15

            binding.btnSelectTime.text = formatTime(dose.timeInMinutes)
            binding.etQuantity.setText(dose.quantity.toString())
            binding.etReminderMinutes.setText(reminderMinutesBefore.toString())
            binding.switchActive.isChecked = dose.isActive
            binding.btnSave.text = "Update"

            // Removed asynchronous database lookup here. Handled in setupMedicineSpinner.
        } ?: run {
            binding.tvTitle.text = "Add Dose Schedule"
            binding.btnSelectTime.text = formatTime(selectedTimeInMinutes)
            binding.etReminderMinutes.setText("15")
            binding.btnSave.text = "Save"
        }
    }

    private fun observeMedicines() {
        medicineStockViewModel.medicineStocks.observe(viewLifecycleOwner) { stocks ->
            medicineStocks = stocks
            // Use coroutine scope to safely run the suspend function
            lifecycleScope.launch {
                setupMedicineSpinner(stocks)
            }
        }
    }

    // FIX: Made suspend to safely perform DB lookups before setting adapter/text
    private suspend fun setupMedicineSpinner(stocks: List<MedicineStock>) {
        val db = AppDatabase.getDatabase(requireContext())
        val newMedicineInfoList = mutableListOf<MedicineInfo>()

        // Fetch all medicine names and stock IDs synchronously within this coroutine
        stocks.forEach { stock ->
            val medicine = db.masterMedicineDao().getById(stock.masterMedicineId)
            val name = medicine?.name ?: "Unknown"
            newMedicineInfoList.add(MedicineInfo(stock.stockId, name))
        }

        medicineInfoList = newMedicineInfoList
        val medicineNames = medicineInfoList.map { it.name }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            medicineNames
        )

        binding.actvMedicine.setAdapter(adapter)

        // FIX: Set pre-selected medicine after adapter is fully loaded (Edit Mode fix)
        existingDose?.let { dose ->
            // Find the medicine name using the stockId from the existing dose
            val selectedName = medicineInfoList.find { it.stockId == dose.stockId }?.name
            if (selectedName != null) {
                // Post to ensure setting text happens after UI is ready
                binding.actvMedicine.post {
                    // Set the text, passing 'false' to suppress filtering (though not strictly necessary here)
                    binding.actvMedicine.setText(selectedName, false)
                    binding.actvMedicine.setSelection(selectedName.length)
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
        val reminderStr = binding.etReminderMinutes.text.toString()

        // Validation
        if (medicineName.isEmpty()) {
            binding.actvMedicine.error = "Select a medicine"
            return
        }

        if (quantityStr.isEmpty()) {
            binding.etQuantity.error = "Quantity required"
            return
        }

        if (reminderStr.isEmpty()) {
            binding.etReminderMinutes.error = "Reminder time required"
            return
        }

        val quantity = quantityStr.toIntOrNull() ?: 1
        val reminderMinutes = reminderStr.toIntOrNull() ?: 15

        // Validate reminder time
        if (reminderMinutes < 0) {
            binding.etReminderMinutes.error = "Must be 0 or more minutes"
            return
        }

        if (reminderMinutes >= 1440) { // 24 hours
            binding.etReminderMinutes.error = "Must be less than 24 hours (1440 minutes)"
            return
        }

        // Find medicine stock ID using the pre-loaded medicineInfoList
        val selectedStockInfo = medicineInfoList.find { info ->
            info.name.equals(medicineName, ignoreCase = true)
        }

        if (selectedStockInfo == null) {
            binding.actvMedicine.error = "Medicine not found or not in stock"
            return
        }

        val stockId = selectedStockInfo.stockId

        val dose = existingDose?.copy(
            stockId = stockId,
            timeInMinutes = selectedTimeInMinutes,
            quantity = quantity,
            reminderMinutesBefore = reminderMinutes,
            isActive = binding.switchActive.isChecked
        ) ?: Dose(
            stockId = stockId,
            patientId = patientId,
            timeInMinutes = selectedTimeInMinutes,
            quantity = quantity,
            reminderMinutesBefore = reminderMinutes,
            isActive = binding.switchActive.isChecked
        )

        onSaveListener?.invoke(dose)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}