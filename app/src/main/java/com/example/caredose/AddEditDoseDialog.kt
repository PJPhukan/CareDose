package com.example.caredose

import android.R
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.caredose.alarm.CareDoseAlarmDoseManager
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Dose
import com.example.caredose.database.entities.MedicineStock
import com.example.caredose.databinding.DialogAddEditDoseBinding
import com.example.caredose.repository.MedicineStockRepository
import com.example.caredose.viewmodels.MedicineStockViewModel
import com.example.caredose.viewmodels.ViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var medicineStocks = listOf<MedicineStock>()
    private var medicineInfoList = listOf<MedicineInfo>()
    private var selectedTimeInMinutes: Int = 540 // Default 9:00 AM
    private var reminderMinutesBefore: Int = 15 // Default 15 minutes

    private lateinit var scheduler: CareDoseAlarmDoseManager
    private lateinit var medicineStockViewModel: MedicineStockViewModel

    companion object {
        private const val TAG = "AddEditDoseDialog"
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
        @Suppress("DEPRECATION")
        existingDose = arguments?.getParcelable(ARG_DOSE)
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

        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        scheduler = CareDoseAlarmDoseManager(requireContext())

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

        medicineStockViewModel =
            ViewModelProvider(this, factory)[MedicineStockViewModel::class.java]
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
            lifecycleScope.launch {
                setupMedicineSpinner(stocks)
            }
        }
    }

    private suspend fun setupMedicineSpinner(stocks: List<MedicineStock>) {
        val db = AppDatabase.getDatabase(requireContext())
        val newMedicineInfoList = mutableListOf<MedicineInfo>()

        stocks.forEach { stock ->
            val medicine = db.masterMedicineDao().getById(stock.masterMedicineId)
            val name = medicine?.name ?: "Unknown"
            newMedicineInfoList.add(MedicineInfo(stock.stockId, name))
        }

        medicineInfoList = newMedicineInfoList
        val medicineNames = medicineInfoList.map { it.name }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_dropdown_item_1line,
            medicineNames
        )

        binding.actvMedicine.setAdapter(adapter)

        existingDose?.let { dose ->
            val selectedName = medicineInfoList.find { it.stockId == dose.stockId }?.name
            if (selectedName != null) {
                binding.actvMedicine.post {
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
            saveDoseAndScheduleAlarm()
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
            false
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

    private fun saveDoseAndScheduleAlarm() {
        val medicineName = binding.actvMedicine.text.toString().trim()
        val quantityStr = binding.etQuantity.text.toString()
        val reminderStr = binding.etReminderMinutes.text.toString()

        if (medicineName.isEmpty() || quantityStr.isEmpty() || reminderStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toIntOrNull() ?: 1
        val reminderMinutes = reminderStr.toIntOrNull() ?: 15

        if (reminderMinutes < 0 || reminderMinutes >= 1440) {
            binding.etReminderMinutes.error = "Invalid reminder time"
            return
        }

        val selectedStockInfo = medicineInfoList.find { info ->
            info.name.equals(medicineName, ignoreCase = true)
        } ?: run {
            binding.actvMedicine.error = "Medicine not found or not in stock"
            return
        }

        val stockId = selectedStockInfo.stockId

        // 1. Create the Dose object
        val newOrUpdatedDose = existingDose?.copy(
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

        // 2. Invoke the listener to save/update the dose in the database
        onSaveListener?.invoke(newOrUpdatedDose)

        lifecycleScope.launch {
            Log.d(TAG, "saveDoseAndScheduleAlarm: started lifecycleScope")
            try {
                withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(requireContext())
                    val stock = db.medicineStockDao().getById(newOrUpdatedDose.stockId)
                    Log.d(TAG, "lifecycleScope try get stock $stock")
                    val patient = db.patientDao().getById(patientId)
                    Log.d(TAG, "lifecycleScope try get patient $patient")

                    if (patient != null) {
                        scheduler.scheduleReminderDose(
                            newOrUpdatedDose,
                            selectedStockInfo.name,
                            patient.name,
                            stock!!.masterMedicineId
                        )
                        Log.d(TAG, "Alarm scheduled/updated for Dose ID ${newOrUpdatedDose.doseId}")
                    } else {
                        Log.e(
                            TAG,
                            "Could not schedule alarm: Patient not found for ID ${newOrUpdatedDose.patientId}"
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    dismiss()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during alarm scheduling: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Dose saved but alarm scheduling failed",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}