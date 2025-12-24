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
    private var isItemSelected = false
    private var selectedMasterVital: MasterVital? = null

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
            masterVitalViewModel.loadVitalsLiveData(userId as Long)

            masterVitalViewModel.allVitals.observe(viewLifecycleOwner) { vitals ->
                masterVitals = vitals
                setupAutoComplete(vitals)

                existingVital?.let { vital ->
                    val vitalName = vitals.find { it.vitalId == vital.masterVitalId }?.name ?: ""
                    val masterVital = vitals.find { it.vitalId == vital.masterVitalId }

                    binding.actvVitalType.setText(vitalName, false)

                    if (masterVital != null) {
                        selectedMasterVital = masterVital
                        binding.etUnit.setText(masterVital.unit)
                        binding.etUnit.isEnabled = false
                        binding.tilUnit.visibility = View.VISIBLE
                    }

                    val unit = masterVital?.unit ?: "unit"
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

        textWatcher = binding.actvVitalType.addTextChangedListener { editable ->

            if (isItemSelected) {
                isItemSelected = false
                return@addTextChangedListener
            }

            val input = editable.toString().trim()
            val listForAdapter = mutableListOf<String>()

            if (input.isNotEmpty()) {
                val matchingVitals =
                    vitalDisplayNames.filter { it.contains(input, ignoreCase = true) }
                listForAdapter.addAll(matchingVitals)

                val vitalNamesLower =
                    matchingVitals.map { it.substringBefore(" (").trim().lowercase() }.toSet()
                val matchingMedicines = medicines
                    .filter { it.contains(input, ignoreCase = true) }
                    .filter { med -> med.trim().lowercase() !in vitalNamesLower }
                listForAdapter.addAll(matchingMedicines)

                listForAdapter.add("➕ Add \"$input\" to vital types")

            } else {
                listForAdapter.addAll(vitalDisplayNames)
            }


            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listForAdapter
            )

            binding.actvVitalType.post {
                binding.actvVitalType.setAdapter(adapter)

                if (input.isNotEmpty() && binding.actvVitalType.hasFocus()) {
                    binding.actvVitalType.showDropDown()
                }
            }
        }

        binding.actvVitalType.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String

            isItemSelected = true

            textWatcher?.let { binding.actvVitalType.removeTextChangedListener(it) }

            when {
                selectedItem.startsWith("➕ Add") -> {
                    val input = selectedItem
                        .substringAfter("➕ Add \"")
                        .substringBefore("\" to vital types")
                        .trim()

                    if (input.isNotEmpty()) {
                        binding.actvVitalType.setText(input, false)

                        selectedMasterVital = null
                        binding.tilUnit.visibility = View.VISIBLE
                        binding.etUnit.isEnabled = true
                        binding.etUnit.setText("")
                        binding.etValue.hint = "Value"

                    }
                }

                selectedItem.contains(" (") && selectedItem.endsWith(")") -> {
                    val vitalName = selectedItem.substringBefore(" (").trim()

                    binding.actvVitalType.setText(vitalName, false)

                    val selectedVital =
                        masterVitals.find { it.name.equals(vitalName, ignoreCase = true) }

                    if (selectedVital != null) {
                        selectedMasterVital = selectedVital
                        binding.tilUnit.visibility = View.VISIBLE
                        binding.etUnit.setText(selectedVital.unit)
                        binding.etUnit.isEnabled = false
                        binding.etValue.hint = "Value (${selectedVital.unit})"
                    }
                }

                else -> {
                  binding.actvVitalType.setText(selectedItem, false)
                    selectedMasterVital = null
                    binding.tilUnit.visibility = View.VISIBLE
                    binding.etUnit.isEnabled = true
                    binding.etUnit.setText("")
                    binding.etValue.hint = "Value (dose / mg / etc.)"
                }
            }

           binding.actvVitalType.dismissDropDown()
            binding.actvVitalType.clearFocus()

          if (binding.etUnit.isEnabled) {
                binding.etUnit.requestFocus()
            } else {
                binding.etValue.requestFocus()
            }

            binding.actvVitalType.postDelayed({
                textWatcher?.let { binding.actvVitalType.addTextChangedListener(it) }
            }, 200)
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

       val newVital = MasterVital(
            userId = userId as Long,
            name = input,
            unit = "unit"
        )

        masterVitals = masterVitals + newVital
  refreshAutoCompleteAdapter()

       binding.actvVitalType.setText(input, false)
        binding.actvVitalType.post { binding.actvVitalType.showDropDown() }

     lifecycleScope.launch {
            try {
                val newId = masterVitalViewModel.addVitalAndGetId(newVital)
               masterVitals = masterVitals.map { mv ->
                    if (mv.name.equals(
                            input,
                            ignoreCase = true
                        ) && (mv.vitalId == 0L || mv.vitalId == -1L)
                    ) {
                        mv.copy(vitalId = newId)
                    } else mv
                }
                binding.actvVitalType.post {
                    refreshAutoCompleteAdapter()
                 binding.actvVitalType.showDropDown()
                }
            } catch (e: Exception) {
                // todo:
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
        val unit = binding.etUnit.text.toString().trim()
        val userId = sessionManager.getUserId()

        // Validation
        if (vitalName.isEmpty()) {
            binding.actvVitalType.error = "Vital type required"
            return
        }

        if (unit.isEmpty()) {
            binding.etUnit.error = "Unit required"
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
            return
        }

        lifecycleScope.launch {
            var masterVitalId: Long

            if (selectedMasterVital != null) {
                masterVitalId = selectedMasterVital!!.vitalId
            } else {
                val existingVital = masterVitals.find {
                    it.name.equals(vitalName, ignoreCase = true)
                }

                if (existingVital != null) {
                    masterVitalId = existingVital.vitalId
                } else {
                    val newVital = MasterVital(
                        userId = userId as Long,
                        name = vitalName,
                        unit = unit
                    )
                    masterVitalId = masterVitalViewModel.addVitalAndGetId(newVital)
                }
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
        textWatcher?.let {
            binding.actvVitalType.removeTextChangedListener(it)
        }
        _binding = null
    }
}