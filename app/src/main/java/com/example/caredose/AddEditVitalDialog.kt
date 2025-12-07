package com.example.caredose.ui.patient.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.caredose.SessionManager
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MasterVital
import com.example.caredose.database.entities.Vital
import com.example.caredose.databinding.DialogAddEditVitalBinding
import com.example.caredose.repository.MasterVitalRepository
import com.example.caredose.viewmodels.MasterVitalViewModel
import com.example.caredose.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddEditVitalDialog : DialogFragment() {

    private var _binding: DialogAddEditVitalBinding? = null
    private val binding get() = _binding!!

    private var patientId: Long = 0
    private var existingVital: Vital? = null
    private var onSaveListener: ((Vital) -> Unit)? = null
    private var textWatcher: TextWatcher? = null
    private lateinit var masterVitalViewModel: MasterVitalViewModel
    private lateinit var sessionManager: SessionManager
    private var masterVitals = listOf<MasterVital>()
    private var selectedTimestamp: Long = System.currentTimeMillis()

    companion object {
        private const val ARG_PATIENT_ID = "patient_id"
        private const val ARG_VITAL = "vital"

        fun newInstance(patientId: Long, vital: Vital? = null): AddEditVitalDialog {
            return AddEditVitalDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PATIENT_ID, patientId)
                    vital?.let { putParcelable(ARG_VITAL, it) }
                }
            }
        }
    }

    fun setOnSaveListener(listener: (Vital) -> Unit) {
        onSaveListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        patientId = arguments?.getLong(ARG_PATIENT_ID) ?: 0

        // Handle both old and new Android versions
        existingVital =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                arguments?.getParcelable(ARG_VITAL, Vital::class.java)
            } else {
                @Suppress("DEPRECATION")
                arguments?.getParcelable(ARG_VITAL)
            }
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
        _binding = DialogAddEditVitalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set dialog width to 90% of screen width
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        sessionManager = SessionManager(requireContext())
        setupViewModel()
        setupUI()
        setupClickListeners()
        loadMasterVitals()
    }

    private fun setupViewModel() {
        val db = AppDatabase.getDatabase(requireContext())
        val factory = ViewModelFactory(
            masterVitalRepository = MasterVitalRepository(db)
        )

        masterVitalViewModel = ViewModelProvider(this, factory)[MasterVitalViewModel::class.java]
    }

    private fun setupUI() {
        existingVital?.let { vital ->
            binding.tvTitle.text = "Edit Vital Reading"
            selectedTimestamp = vital.recordedAt

            // Will set vital name after vitals are loaded
            binding.etValue.setText(vital.value.toString())
            binding.etNote.setText(vital.note ?: "")
            binding.btnSelectDate.text = formatDate(vital.recordedAt)
            binding.btnSave.text = "Update"
        } ?: run {
            binding.tvTitle.text = "Add Vital Reading"
            binding.btnSelectDate.text = formatDate(selectedTimestamp)
            binding.btnSave.text = "Save"
        }
    }

    private fun getVitalNameWithUnit(vitalId: Long): String {
        val vital = masterVitals.find { it.vitalId == vitalId }
        return if (vital != null) {
            "${vital.name} (${vital.unit})"
        } else {
            ""
        }
    }

    private fun loadMasterVitals() {
        val userId = sessionManager.getUserId()
        if (userId != -1L) {
            // Load vitals using LiveData
            masterVitalViewModel.loadVitalsLiveData(userId as Long)

            // Observe the LiveData
            masterVitalViewModel.allVitals.observe(viewLifecycleOwner) { vitals ->
                masterVitals = vitals
                setupAutoComplete(vitals)

                // Set vital name after vitals are loaded (for edit mode)
                existingVital?.let { vital ->
                    val vitalName = vitals.find { it.vitalId == vital.masterVitalId }?.name ?: ""
                    binding.actvVitalType.setText(vitalName)

                    // Update value hint with unit
                    val unit = vitals.find { it.vitalId == vital.masterVitalId }?.unit ?: "unit"
                    binding.etValue.hint = "Value ($unit)"
                }
            }
        }
    }

    private fun setupAutoComplete(
        vitals: List<MasterVital>,
        medicines: List<String> = emptyList()
    ) {
        val vitalDisplayNames = vitals.map { "${it.name} (${it.unit})" }
        binding.actvVitalType.threshold = 1

        // 1. Define the TextWatcher explicitly
        textWatcher = binding.actvVitalType.addTextChangedListener { editable ->
            val input = editable.toString().trim()
            val listForAdapter = mutableListOf<String>()

            if (input.isNotEmpty()) {
                // 1) matching vitals (show with unit)
                val matchingVitals =
                    vitalDisplayNames.filter { it.contains(input, ignoreCase = true) }
                listForAdapter.addAll(matchingVitals)

                // 2) matching medicines (plain)
                val vitalNamesLower =
                    matchingVitals.map { it.substringBefore(" (").trim().lowercase() }.toSet()
                val matchingMedicines = medicines
                    .filter { it.contains(input, ignoreCase = true) }
                    .filter { med -> med.trim().lowercase() !in vitalNamesLower }
                listForAdapter.addAll(matchingMedicines)

                // 3) ALWAYS append Add option when input not empty
                listForAdapter.add("➕ Add \"$input\" to vital types")

            } else {
                // when input empty: show all vitals (no Add)
                listForAdapter.addAll(vitalDisplayNames)
            }

            // Adapter setup
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listForAdapter
            )

            // Posting the UI updates to avoid blinking/flickering
            binding.actvVitalType.post {
                binding.actvVitalType.setAdapter(adapter)

                if (input.isNotEmpty()) {
                    binding.actvVitalType.showDropDown()
                }
            }
        }

        // 2. The rest of setupAutoComplete (setOnItemClickListener)
        binding.actvVitalType.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String

            // Temporarily remove the TextWatcher
            binding.actvVitalType.removeTextChangedListener(textWatcher!!)

            when {
                selectedItem.startsWith("➕ Add") -> {
                    // Extract the clean input
                    val input = selectedItem
                        .substringAfter("➕ Add \"")
                        .substringBefore("\" to vital types")
                        .trim()

                    if (input.isNotEmpty()) {
                        // Set the clean text without triggering the listener
                        binding.actvVitalType.setText(input)
                        binding.actvVitalType.setSelection(input.length)

                        // Then call the logic to handle saving/updating master list
                        addToMasterList(input)
                    }
                }

                selectedItem.contains(" (") && selectedItem.endsWith(")") -> {
                    val vitalName = selectedItem.substringBefore(" (").trim()

                    // Set the clean vital name without triggering the listener
                    binding.actvVitalType.setText(vitalName)

                    val selectedVital =
                        masterVitals.find { it.name.equals(vitalName, ignoreCase = true) }
                    selectedVital?.let { binding.etValue.hint = "Value (${it.unit})" }
                }

                else -> {
                    // medicine selected
                    binding.actvVitalType.setText(selectedItem)
                    binding.etValue.hint = "Value (dose / mg / etc.)"
                }
            }

            // Re-add the TextWatcher
            binding.actvVitalType.addTextChangedListener(textWatcher!!)

            // Crucial: Manually hide the dropdown to prevent cursor movement from showing it again
            binding.actvVitalType.dismissDropDown()


            binding.etValue.requestFocus()
        }

        // initial adapter
        val initialAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            vitalDisplayNames
        )
        binding.actvVitalType.setAdapter(initialAdapter)
    }

    private fun setupClickListeners() {
        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            saveVital()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun addToMasterList(name: String) {
        val userId = sessionManager.getUserId()
        val input = name.trim()
        if (input.isEmpty() || userId == -1L) return

        // Create a new MasterVital object (id=0 until DB returns real id)
        val newVital = MasterVital(
            userId = userId as Long,
            name = input,
            unit = "unit"
        )

        // 1) Update local list immediately to avoid UI flicker
        //    We'll append a placeholder with vitalId = 0 (or -1) and update after DB returns if needed.
        masterVitals = masterVitals + newVital

        // 2) Immediately update adapter so dropdown can use new data
        refreshAutoCompleteAdapter()

        // Keep the typed text and caret at end, then re-open dropdown after UI settles
        binding.actvVitalType.setText(input)
        binding.actvVitalType.setSelection(input.length)
        binding.actvVitalType.post { binding.actvVitalType.showDropDown() }

        // 3) Persist to DB / ViewModel asynchronously and update local entry with real id
        lifecycleScope.launch {
            try {
                val newId = masterVitalViewModel.addVitalAndGetId(newVital)
                // Replace the placeholder entry with real id (match by name)
                masterVitals = masterVitals.map { mv ->
                    if (mv.name.equals(
                            input,
                            ignoreCase = true
                        ) && (mv.vitalId == 0L || mv.vitalId == -1L)
                    ) {
                        mv.copy(vitalId = newId)
                    } else mv
                }
                // Refresh adapter again to include correct ids/units (if unit changed)
                binding.actvVitalType.post {
                    refreshAutoCompleteAdapter()
                    // keep dropdown visible so the user still sees suggestions
                    binding.actvVitalType.showDropDown()
                }
            } catch (e: Exception) {
                // handle error (optional): remove placeholder or notify user
            }
        }
    }

    private fun refreshAutoCompleteAdapter(medicines: List<String> = emptyList()) {
        val vitalDisplayNames = masterVitals.map { "${it.name} (${it.unit})" }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            vitalDisplayNames
        )
        binding.actvVitalType.setAdapter(adapter)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedTimestamp
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedTimestamp = calendar.timeInMillis
                binding.btnSelectDate.text = formatDate(selectedTimestamp)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun saveVital() {
        val vitalName = binding.actvVitalType.text.toString().trim()
        val valueStr = binding.etValue.text.toString()
        val note = binding.etNote.text.toString().trim()
        val userId = sessionManager.getUserId()

        // Validation
        if (vitalName.isEmpty()) {
            binding.actvVitalType.error = "Vital type required"
            return
        }

        if (valueStr.isEmpty()) {
            binding.etValue.error = "Value required"
            return
        }

        val value = valueStr.toDoubleOrNull()
        if (value == null) {
            binding.etValue.error = "Invalid value"
            return
        }

        if (userId == -1L) {
            // Handle error: user not logged in
            return
        }

        // Find or create master vital
        lifecycleScope.launch {
            var masterVitalId = masterVitals.find {
                it.name.equals(vitalName, ignoreCase = true)
            }?.vitalId

            if (masterVitalId == null) {
                // Auto-add to master list
                val newVital = MasterVital(
                    userId = userId as Long,
                    name = vitalName,
                    unit = "unit" // Default unit
                )
                masterVitalId = masterVitalViewModel.addVitalAndGetId(newVital)
            }

            val vital = existingVital?.copy(
                masterVitalId = masterVitalId,
                value = value,
                recordedAt = selectedTimestamp,
                note = note.ifEmpty { null }
            ) ?: Vital(
                patientId = patientId,
                masterVitalId = masterVitalId,
                value = value,
                recordedAt = selectedTimestamp,
                note = note.ifEmpty { null }
            )

            onSaveListener?.invoke(vital)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}