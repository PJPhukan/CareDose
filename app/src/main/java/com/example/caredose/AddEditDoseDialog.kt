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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.caredose.alarm.CareDoseAlarmDoseManager
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Dose
import com.example.caredose.database.entities.DurationType
import com.example.caredose.database.entities.MedicineStock
import com.example.caredose.databinding.DialogAddEditDoseBinding
import com.example.caredose.repository.MedicineStockRepository
import com.example.caredose.viewmodels.MedicineStockViewModel
import com.example.caredose.viewmodels.ViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

private data class MedicineInfo(
    val stockId: Long,
    val medicineId: Long,
    val name: String
)

class AddEditDoseDialog : DialogFragment() {
    private var _binding: DialogAddEditDoseBinding? = null
    private val binding get() = _binding!!
    private var patientId: Long = 0
    private var userId: Long = 0
    private var existingDose: Dose? = null
    private var onSaveListener: ((Dose, (Long) -> Unit) -> Unit)? = null
    private var medicineStocks = listOf<MedicineStock>()
    private var medicineInfoList = listOf<MedicineInfo>()
    private var selectedTimeInMinutes: Int = 540
    private var reminderMinutesBefore: Int = 15

    private lateinit var scheduler: CareDoseAlarmDoseManager
    private lateinit var medicineStockViewModel: MedicineStockViewModel

    // Duration fields
    private var selectedDurationType: DurationType = DurationType.DAYS
    private var durationValue: Int = 7

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

    fun setOnSaveListener(listener: (Dose, (Long) -> Unit) -> Unit) {
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

        setupUI()
        setupDurationUI()
        setupClickListeners()
        setupViewModelAndObserve()
    }

    private fun setupViewModelAndObserve() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val patient = db.patientDao().getById(patientId)
            userId = patient?.userId ?: 0

            withContext(Dispatchers.Main) {
                val factory = ViewModelFactory(
                    medicineStockRepository = MedicineStockRepository(db.medicineStockDao())
                )

                medicineStockViewModel =
                    ViewModelProvider(
                        this@AddEditDoseDialog,
                        factory
                    )[MedicineStockViewModel::class.java]
                medicineStockViewModel.setUserId(userId)

                observeMedicines()
            }
        }
    }

    private fun setupUI() {
        existingDose?.let { dose ->
            binding.tvTitle.text = "Edit Dose Schedule"
            selectedTimeInMinutes = dose.timeInMinutes
            reminderMinutesBefore = dose.reminderMinutesBefore

            binding.btnSelectTime.text = formatTime(dose.timeInMinutes)
            binding.etQuantity.setText(dose.quantity.toString())
            binding.etReminderMinutes.setText(reminderMinutesBefore.toString())
            binding.switchActive.isChecked = dose.isActive
            binding.btnSave.text = "Update"

            // Load duration from existing dose
            selectedDurationType = dose.getDurationTypeEnum()
            durationValue = dose.durationValue ?: 7
        } ?: run {
            binding.tvTitle.text = "Add Dose Schedule"
            binding.btnSelectTime.text = formatTime(selectedTimeInMinutes)
            binding.etReminderMinutes.setText("15")
            binding.btnSave.text = "Save"
        }
    }

    private fun setupDurationUI() {
        // Setup duration type spinner
        val durationTypes = DurationType.getDisplayNames()
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item,
            durationTypes
        )
        spinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinnerDurationType.adapter = spinnerAdapter

        // Set initial selection
        val initialTypeIndex = durationTypes.indexOf(selectedDurationType.displayName)
        if (initialTypeIndex >= 0) {
            binding.spinnerDurationType.setSelection(initialTypeIndex)
        }

        // Set initial duration value
        binding.etDurationValue.setText(durationValue.toString())

        // Handle radio button changes
        existingDose?.let { dose ->
            if (dose.durationType == DurationType.CONTINUOUS.name) {
                binding.rbContinuous.isChecked = true
                binding.rbSpecificDuration.isChecked = false
                binding.layoutDurationValue.visibility = View.GONE
                binding.tvDurationPreview.visibility = View.GONE
            } else {
                binding.rbSpecificDuration.isChecked = true
                binding.rbContinuous.isChecked = false
                binding.layoutDurationValue.visibility = View.VISIBLE
                updateDurationPreview()
            }
        } ?: run {
            // Default: Specific duration selected
            binding.rbSpecificDuration.isChecked = true
            binding.layoutDurationValue.visibility = View.VISIBLE
            updateDurationPreview()
        }

        binding.rgDurationType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.rbContinuous.id -> {
                    binding.layoutDurationValue.visibility = View.GONE
                    binding.tvDurationPreview.visibility = View.GONE
                    selectedDurationType = DurationType.CONTINUOUS
                }

                binding.rbSpecificDuration.id -> {
                    binding.layoutDurationValue.visibility = View.VISIBLE
                    updateDurationPreview()
                }
            }
        }

        // Update preview when duration value changes
        binding.etDurationValue.addTextChangedListener {
            if (binding.rbSpecificDuration.isChecked) {
                updateDurationPreview()
            }
        }

        // Update preview when duration type changes
        binding.spinnerDurationType.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    selectedDurationType = DurationType.fromDisplayName(durationTypes[position])
                    if (binding.rbSpecificDuration.isChecked) {
                        updateDurationPreview()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun updateDurationPreview() {
        val valueStr = binding.etDurationValue.text.toString()
        val value = valueStr.toIntOrNull()

        if (value != null && value > 0) {
            val startDate = System.currentTimeMillis()
            val endDate = DurationHelper.calculateEndDate(selectedDurationType, value, startDate)

            if (endDate != null) {
                val summary =
                    DurationHelper.getDurationSummary(selectedDurationType, value, endDate)
                binding.tvDurationPreview.text = "Schedule will $summary"
                binding.tvDurationPreview.visibility = View.VISIBLE
            }
        } else {
            binding.tvDurationPreview.visibility = View.GONE
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
            newMedicineInfoList.add(
                MedicineInfo(
                    stockId = stock.stockId,
                    medicineId = stock.masterMedicineId,
                    name = name
                )
            )
        }

        medicineInfoList = newMedicineInfoList
        val medicineNames = medicineInfoList.map { it.name }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_dropdown_item_1line,
            medicineNames
        )

        binding.actvMedicine.setAdapter(adapter)

        binding.actvMedicine.setOnItemClickListener { _, _, _, _ ->
            binding.btnSelectTime.requestFocus()
        }
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

        // Validate duration
        val isContinuous = binding.rbContinuous.isChecked
        val durationType = if (isContinuous) {
            DurationType.CONTINUOUS
        } else {
            selectedDurationType
        }

        val durationVal = if (isContinuous) {
            null
        } else {
            binding.etDurationValue.text.toString().toIntOrNull()
        }

        if (!isContinuous) {
            val validationError = DurationHelper.validateDurationValue(durationType, durationVal)
            if (validationError != null) {
                binding.etDurationValue.error = validationError
                return
            }
        }

        val selectedStockInfo = medicineInfoList.find { info ->
            info.name.equals(medicineName, ignoreCase = true)
        } ?: run {
            binding.actvMedicine.error = "Medicine not found or not in stock"
            return
        }

        val stockId = selectedStockInfo.stockId
        val medicineId = selectedStockInfo.medicineId

        // ========================================
        // Check for duplicate medicine schedule
        // ========================================
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val currentTime = System.currentTimeMillis()

                val existingDoses = db.doseDao().getActiveDosesForPatientList(patientId, currentTime)

                val duplicateDose = existingDoses.firstOrNull { dose ->
                    dose.medicineId == medicineId && dose.doseId != existingDose?.doseId
                }

                withContext(Dispatchers.Main) {
                    if (duplicateDose != null && existingDose == null) {
                        // Check if durations match
                        val existingType = duplicateDose.getDurationTypeEnum()
                        val existingValue = duplicateDose.durationValue

                        val durationsMatch = (existingType == durationType) &&
                                (existingValue == durationVal)

                        if (durationsMatch) {
                           proceedWithSave(
                                stockId = stockId,
                                medicineId = medicineId,
                                medicineName = medicineName,
                                quantity = quantity,
                                reminderMinutes = reminderMinutes,
                                durationType = durationType,
                                durationVal = durationVal,
                                replaceExisting = false
                            )
                        } else {
                        showDuplicateScheduleDialog(
                                medicineName = medicineName,
                                stockId = stockId,
                                medicineId = medicineId,
                                quantity = quantity,
                                reminderMinutes = reminderMinutes,
                                durationType = durationType,
                                durationVal = durationVal,
                                existingDurationType = existingType,
                                existingDurationValue = existingValue
                            )
                        }
                    } else {
                        // No duplicate or editing existing dose
                        proceedWithSave(
                            stockId = stockId,
                            medicineId = medicineId,
                            medicineName = medicineName,
                            quantity = quantity,
                            reminderMinutes = reminderMinutes,
                            durationType = durationType,
                            durationVal = durationVal,
                            replaceExisting = false
                        )
                    }
                }
            } catch (e: Exception) {
               withContext(Dispatchers.Main) {
                    proceedWithSave(
                        stockId = stockId,
                        medicineId = medicineId,
                        medicineName = medicineName,
                        quantity = quantity,
                        reminderMinutes = reminderMinutes,
                        durationType = durationType,
                        durationVal = durationVal,
                        replaceExisting = false
                    )
                }
            }
        }
    }

    private fun proceedWithSave(
        stockId: Long,
        medicineId: Long,
        medicineName: String,
        quantity: Int,
        reminderMinutes: Int,
        durationType: DurationType,
        durationVal: Int?,
        replaceExisting: Boolean
    ) {
        lifecycleScope.launch {
            try {
                // If replacing existing, deactivate old schedules
                if (replaceExisting) {
                    deactivateExistingSchedules(medicineId, patientId)
                }

                // Calculate dates
                val startDate = System.currentTimeMillis()
                val endDate = if (durationType == DurationType.CONTINUOUS) {
                    null
                } else {
                    DurationHelper.calculateEndDate(durationType, durationVal, startDate)
                }

                // Generate or reuse scheduleGroupId
                val scheduleGroupId = existingDose?.scheduleGroupId
                    ?: DurationHelper.generateScheduleGroupId()

                // Create dose with duration fields
                val newOrUpdatedDose = existingDose?.copy(
                    stockId = stockId,
                    patientId = patientId,
                    medicineId = medicineId,
                    timeInMinutes = selectedTimeInMinutes,
                    quantity = quantity,
                    reminderMinutesBefore = reminderMinutes,
                    isActive = binding.switchActive.isChecked,
                    durationType = durationType.name,
                    durationValue = durationVal,
                    startDate = startDate,
                    endDate = endDate
                ) ?: Dose(
                    scheduleGroupId = scheduleGroupId,
                    stockId = stockId,
                    patientId = patientId,
                    medicineId = medicineId,
                    timeInMinutes = selectedTimeInMinutes,
                    quantity = quantity,
                    reminderMinutesBefore = reminderMinutes,
                    isActive = binding.switchActive.isChecked,
                    durationType = durationType.name,
                    durationValue = durationVal,
                    startDate = startDate,
                    endDate = endDate
                )

                onSaveListener?.invoke(newOrUpdatedDose) { savedDoseId ->
                    lifecycleScope.launch {

                        try {
                            withContext(Dispatchers.IO) {
                                val db = AppDatabase.getDatabase(requireContext())

                                val savedDose = db.doseDao().getById(savedDoseId)
                                val stock = db.medicineStockDao().getById(stockId)
                                val patient = db.patientDao().getById(patientId)

                                if (savedDose != null && stock != null && patient != null) {
                                    withContext(Dispatchers.Main) {
                                        scheduler.scheduleReminderDose(
                                            savedDose,
                                            medicineName,
                                            patient.name,
                                            medicineId = medicineId
                                        )

                                    }
                                } else {
                                    Log.e(
                                        TAG,
                                        "Could not schedule alarm: Dose, Stock or Patient not found"
                                    )
                                }
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    requireContext(),
                                    "Dose saved and alarm scheduled",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismiss()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()

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
            } catch (e: Exception) {
               withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Error saving dose: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showDuplicateScheduleDialog(
        medicineName: String,
        stockId: Long,
        medicineId: Long,
        quantity: Int,
        reminderMinutes: Int,
        durationType: DurationType,
        durationVal: Int?,
        existingDurationType: DurationType?,
        existingDurationValue: Int?
    ) {
        // Build message with duration details
        val currentDurationText = if (durationType == DurationType.CONTINUOUS) {
            "Continuous (no end date)"
        } else {
            "$durationVal ${durationType.displayName}"
        }

        val existingDurationText = if (existingDurationType == DurationType.CONTINUOUS) {
            "Continuous (no end date)"
        } else {
            "$existingDurationValue ${existingDurationType?.displayName}"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Medicine Already Scheduled")
            .setMessage(
                "This patient already has an active schedule for $medicineName with different duration.\n\n" +
                        "Existing duration: $existingDurationText\n" +
                        "Your selected duration: $currentDurationText\n\n" +
                        "What would you like to do?\n\n" +
                        "• Keep Existing - Create schedule with existing duration ($existingDurationText)\n" +
                        "• Keep Both - Create both schedules with different durations\n" +
                        "• Replace - Stop old and create with new duration ($currentDurationText)"
            )
            .setPositiveButton("Keep Existing") { dialog, _ ->
                // Create new dose with existing duration
                proceedWithSave(
                    stockId = stockId,
                    medicineId = medicineId,
                    medicineName = medicineName,
                    quantity = quantity,
                    reminderMinutes = reminderMinutes,
                    durationType = existingDurationType ?: DurationType.DAYS,
                    durationVal = existingDurationValue,
                    replaceExisting = false
                )
                dialog.dismiss()
            }
            .setNeutralButton("Keep Both") { _, _ ->
                proceedWithSave(
                    stockId = stockId,
                    medicineId = medicineId,
                    medicineName = medicineName,
                    quantity = quantity,
                    reminderMinutes = reminderMinutes,
                    durationType = durationType,
                    durationVal = durationVal,
                    replaceExisting = false
                )
            }
            .setNegativeButton("Replace") { _, _ ->
                // Replace existing with new duration
                proceedWithSave(
                    stockId = stockId,
                    medicineId = medicineId,
                    medicineName = medicineName,
                    quantity = quantity,
                    reminderMinutes = reminderMinutes,
                    durationType = durationType,
                    durationVal = durationVal,
                    replaceExisting = true
                )
            }
            .setCancelable(false)
            .show()
    }

    private suspend fun deactivateExistingSchedules(medicineId: Long, patientId: Long) {
        val db = AppDatabase.getDatabase(requireContext())
        val currentTime = System.currentTimeMillis()

        val existingDoses = db.doseDao().getActiveDosesForPatientList(patientId, currentTime)

        existingDoses
            .filter { it.medicineId == medicineId }
            .forEach { dose ->
                db.doseDao().deactivateScheduleGroup(dose.scheduleGroupId)
                scheduler.cancelScheduleReminder(dose)
         }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}