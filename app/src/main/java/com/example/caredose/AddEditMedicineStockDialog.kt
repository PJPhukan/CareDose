package com.example.caredose.ui.patient.dialogs

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
import com.example.caredose.States
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MasterMedicine
import com.example.caredose.database.entities.MedicineStock
import com.example.caredose.databinding.DialogAddEditMedicineStockBinding
import com.example.caredose.repository.MasterMedicineRepository
import com.example.caredose.viewmodels.MasterMedicineViewModel
import com.example.caredose.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch

class AddEditMedicineStockDialog : DialogFragment() {

    private var _binding: DialogAddEditMedicineStockBinding? = null
    private val binding get() = _binding!!

    private var patientId: Long = -1L
    private var existingStock: MedicineStock? = null
    private var onSaveListener: ((MedicineStock) -> Unit)? = null

    private lateinit var masterMedicineViewModel: MasterMedicineViewModel
    private lateinit var sessionManager: SessionManager
    private var masterMedicines = listOf<MasterMedicine>()

    // 1. Define the TextWatcher property
    private var textWatcher: TextWatcher? = null

    companion object {
        private const val ARG_PATIENT_ID = "patient_id"
        private const val ARG_STOCK = "existing_stock"

        fun newInstance(patientId: Long, stock: MedicineStock? = null): AddEditMedicineStockDialog {
            return AddEditMedicineStockDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_PATIENT_ID, patientId)
                    stock?.let {
                        putParcelable(ARG_STOCK, it)
                    }
                }
            }
        }
    }

    fun setOnSaveListener(listener: (MedicineStock) -> Unit) {
        onSaveListener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        patientId = arguments?.getLong(ARG_PATIENT_ID) ?: -1L

        // Handle both old and new Android versions
        existingStock = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_STOCK, MedicineStock::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_STOCK)
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
        _binding = DialogAddEditMedicineStockBinding.inflate(inflater, container, false)
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
        loadMasterMedicines()
    }

    private fun setupViewModel() {
        val db = AppDatabase.getDatabase(requireContext())
        val factory = ViewModelFactory(
            masterMedicineRepository = MasterMedicineRepository(db)
        )

        masterMedicineViewModel = ViewModelProvider(this, factory)[MasterMedicineViewModel::class.java]
    }

    private fun setupUI() {
        existingStock?.let { stock ->
            binding.tvTitle.text = "Edit Medicine Stock"
            // Set all fields from existing stock
            binding.etStockQty.setText(stock.stockQty.toString())
            binding.etStockThreshold.setText(stock.reminderStockThreshold.toString())
            binding.etDuration.setText(stock.duration.toString())
            binding.switchReminder.isChecked = stock.isReminderEnabled
            binding.btnSave.text = "Update"
        } ?: run {
            binding.tvTitle.text = "Add Medicine Stock"
            binding.btnSave.text = "Save"
        }
    }

    private fun loadMasterMedicines() {
        val userId = sessionManager.getUserId()
        if (userId != -1L) {
            masterMedicineViewModel.loadMedicines(userId as Long)

            viewLifecycleOwner.lifecycleScope.launch {
                masterMedicineViewModel.medicinesState.collect { state ->
                    when (state) {
                        is States.Idle -> {
                            // Do nothing
                        }

                        is States.Loading -> {
                            // Do nothing
                        }

                        is States.Success -> {
                            val medicines = state.data
                            masterMedicines = medicines
                            setupAutoComplete(medicines)

                            // Set medicine name after medicines are loaded
                            existingStock?.let { stock ->
                                val medicineName = medicines.find {
                                    it.medicineId == stock.masterMedicineId
                                }?.name ?: ""
                                binding.actvMedicineName.setText(medicineName)
                            }
                        }

                        is States.Error -> {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }

    private fun setupAutoComplete(medicines: List<MasterMedicine>) {
        val medicineNames = medicines.map { it.name }.toMutableList()
        binding.actvMedicineName.threshold = 1

        // 2. Define and apply the TextWatcher
        textWatcher = binding.actvMedicineName.addTextChangedListener { editable ->
            val input = editable.toString().trim()
            val listForAdapter = mutableListOf<String>()

            // Find matching existing medicine names
            val matchingMedicines = medicineNames.filter { it.contains(input, ignoreCase = true) }
            listForAdapter.addAll(matchingMedicines)

            if (input.isNotEmpty()) {
                // 3. ALWAYS append Add option when input not empty
                listForAdapter.add("➕ Add \"$input\" to medicines")
            }

            // Adapter setup
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                listForAdapter
            )

            // FIX: Posting the UI updates to avoid blinking/flickering
            binding.actvMedicineName.post {
                binding.actvMedicineName.setAdapter(adapter)

                if (input.isNotEmpty()) {
                    binding.actvMedicineName.showDropDown()
                }
            }
        }

        binding.actvMedicineName.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = binding.actvMedicineName.adapter.getItem(position).toString()

            // FIX: Temporarily remove the TextWatcher to prevent re-triggering and dropdown show
            textWatcher?.let { binding.actvMedicineName.removeTextChangedListener(it) }

            if (selectedItem.startsWith("➕ Add")) {
                // FIX: Extract the clean input from the "➕ Add..." string
                val input = selectedItem
                    .substringAfter("➕ Add \"")
                    .substringBefore("\" to medicines")
                    .trim()

                if (input.isNotEmpty()) {
                    // Set the clean text without triggering the listener
                    binding.actvMedicineName.setText(input)
                    binding.actvMedicineName.setSelection(input.length)
                    addToMasterList(input)
                }
            } else {
                // For existing medicine, set the text and selection (already done by AutoComplete)
                // We just need to ensure the text matches the item clicked if necessary
                val input = selectedItem.trim()
                binding.actvMedicineName.setText(input)
                binding.actvMedicineName.setSelection(input.length)
            }

            // FIX: Re-add the TextWatcher
            textWatcher?.let { binding.actvMedicineName.addTextChangedListener(it) }

            // FIX: Manually hide the dropdown and move focus
            binding.actvMedicineName.dismissDropDown()
            binding.etStockQty.requestFocus()
        }

        // Initial adapter (only existing medicine names)
        val initialAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            medicineNames
        )
        binding.actvMedicineName.setAdapter(initialAdapter)
    }

    private fun addToMasterList(name: String) {
        val userId = sessionManager.getUserId()

        if (name.isNotEmpty() && userId != -1L) {
            val newMedicine = MasterMedicine(
                userId = userId as Long,
                name = name.trim()
            )
            // Note: The UI field is already set to the clean name in setOnItemClickListener

            // Persist to DB asynchronously
            masterMedicineViewModel.addMedicine(newMedicine)
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            saveMedicineStock()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun saveMedicineStock() {
        val medicineName = binding.actvMedicineName.text.toString().trim()
        val stockQtyStr = binding.etStockQty.text.toString()
        val thresholdStr = binding.etStockThreshold.text.toString()
        val durationStr = binding.etDuration.text.toString()
        val reminderEnabled = binding.switchReminder.isChecked
        val userId = sessionManager.getUserId()

        // Validation
        if (medicineName.isEmpty()) {
            binding.actvMedicineName.error = "Medicine name required"
            return
        }

        if (stockQtyStr.isEmpty()) {
            binding.etStockQty.error = "Stock quantity required"
            return
        }

        val stockQty = stockQtyStr.toIntOrNull()
        if (stockQty == null || stockQty <= 0) {
            binding.etStockQty.error = "Enter valid quantity"
            return
        }

        val threshold = thresholdStr.toIntOrNull() ?: 5

        if (durationStr.isEmpty()) {
            binding.etDuration.error = "Duration required"
            return
        }
        val duration = durationStr.toIntOrNull()
        if (duration == null || duration <= 0) {
            binding.etDuration.error = "Enter valid duration"
            return
        }
        if (userId == -1L) {
            return
        }

        // Find or create master medicine
        lifecycleScope.launch {
            var masterMedicineId = masterMedicines.find {
                it.name.equals(medicineName, ignoreCase = true)
            }?.medicineId

            if (masterMedicineId == null) {
                // Auto-add to master list
                val newMedicine = MasterMedicine(
                    userId = userId as Long,
                    name = medicineName
                )
                masterMedicineId = masterMedicineViewModel.addMedicineAndGetId(newMedicine)
            }

            val stock = existingStock?.copy(
                masterMedicineId = masterMedicineId,
                stockQty = stockQty,
                reminderStockThreshold = threshold,
                duration = duration,
                isReminderEnabled = reminderEnabled
            ) ?: MedicineStock(
                patientId = patientId,
                masterMedicineId = masterMedicineId,
                stockQty = stockQty,
                reminderStockThreshold = threshold,
                duration = duration,
                isReminderEnabled = reminderEnabled
            )

            onSaveListener?.invoke(stock)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 4. Clean up the TextWatcher
        textWatcher?.let {
            binding.actvMedicineName.removeTextChangedListener(it)
        }
        _binding = null
    }
}