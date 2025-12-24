package com.example.caredose.ui.notifications

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.caredose.SessionManager
import com.example.caredose.States
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MasterMedicine
import com.example.caredose.databinding.DialogAddMedicineBinding
import com.example.caredose.databinding.DialogAddVitalBinding
import com.example.caredose.databinding.FragmentNotificationsBinding
import com.example.caredose.repository.MasterMedicineRepository
import com.example.caredose.repository.MasterVitalRepository
import com.example.caredose.viewmodels.MasterMedicineViewModel
import com.example.caredose.viewmodels.MasterVitalViewModel

import com.example.caredose.viewmodels.ViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sessionManager: SessionManager
    private lateinit var medicineViewModel: MasterMedicineViewModel
    private lateinit var vitalViewModel: MasterVitalViewModel

    private lateinit var medicineFragment: MedicineListFragment
    private lateinit var vitalFragment: VitalListFragment

    private var currentTabPosition = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        setupViewModels()
        setupViewPager()
        setupFabButton()
        observeViewModels()
    }

    private fun setupViewModels() {
        val database = AppDatabase.getDatabase(requireContext())

        val medicineRepo = MasterMedicineRepository(database)
        val vitalRepo = MasterVitalRepository(database)

        val factory = ViewModelFactory(
            masterMedicineRepository = medicineRepo,
            masterVitalRepository = vitalRepo
        )

        medicineViewModel = ViewModelProvider(this, factory)[MasterMedicineViewModel::class.java]
        vitalViewModel = ViewModelProvider(this, factory)[MasterVitalViewModel::class.java]
    }

    private fun setupViewPager() {
        medicineFragment = MedicineListFragment().apply {
            onItemClick = { medicineId, name ->
                showEditMedicineDialog(medicineId, name)
            }
        }

        vitalFragment = VitalListFragment().apply {
            onItemClick = { vitalId, name, unit ->
                showEditVitalDialog(vitalId, name, unit)
            }
        }

        val adapter = MasterPagerAdapter(this, medicineFragment, vitalFragment)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Medicines"
                1 -> "Vitals"
                else -> ""
            }
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })


    }

    private fun setupFabButton() {
        binding.fabAdd.setOnClickListener {
            when (currentTabPosition) {
                0 -> showAddMedicineDialog()
                1 -> showAddVitalDialog()
            }
        }
    }

    private fun showAddMedicineDialog() {
        val dialogBinding = DialogAddMedicineBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = "Add Medicine"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etMedicineName.text.toString().trim()
            if (name.isNotEmpty()) {
                val userId = sessionManager.getUserId() as Long
                medicineViewModel.addMedicine(MasterMedicine(userId, name))
                dialog.dismiss()
            } else {
                dialogBinding.etMedicineName.error = "Medicine name required"
            }
        }

        dialog.show()
    }

    private fun showEditMedicineDialog(medicineId: Long, currentName: String) {
        val dialogBinding = DialogAddMedicineBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = "Edit Medicine"
        dialogBinding.etMedicineName.setText(currentName)
        dialogBinding.btnSave.text = "Update"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etMedicineName.text.toString().trim()
            if (name.isNotEmpty()) {
                val userId = sessionManager.getUserId() as Long
                medicineViewModel.updateMedicine(medicineId, userId, name)
                dialog.dismiss()
            } else {
                dialogBinding.etMedicineName.error = "Medicine name required"
            }
        }

        dialog.show()
    }

    private fun showAddVitalDialog() {
        val dialogBinding = DialogAddVitalBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = "Add Vital"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etVitalName.text.toString().trim()
            val unit = dialogBinding.etVitalUnit.text.toString().trim()

            if (name.isNotEmpty() && unit.isNotEmpty()) {
                val userId = sessionManager.getUserId() as Long
                vitalViewModel.addVital(userId, name, unit)
                dialog.dismiss()
            } else {
                if (name.isEmpty()) dialogBinding.etVitalName.error = "Vital name required"
                if (unit.isEmpty()) dialogBinding.etVitalUnit.error = "Unit required"
            }
        }

        dialog.show()
    }

    private fun showEditVitalDialog(vitalId: Long, currentName: String, currentUnit: String) {
        val dialogBinding = DialogAddVitalBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = "Edit Vital"
        dialogBinding.etVitalName.setText(currentName)
        dialogBinding.etVitalUnit.setText(currentUnit)
        dialogBinding.btnSave.text = "Update"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.etVitalName.text.toString().trim()
            val unit = dialogBinding.etVitalUnit.text.toString().trim()

            if (name.isNotEmpty() && unit.isNotEmpty()) {
                val userId = sessionManager.getUserId() as Long
                vitalViewModel.updateVital(vitalId, userId, name, unit)
                dialog.dismiss()
            } else {
                if (name.isEmpty()) dialogBinding.etVitalName.error = "Vital name required"
                if (unit.isEmpty()) dialogBinding.etVitalUnit.error = "Unit required"
            }
        }

        dialog.show()
    }

    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            medicineViewModel.addEditState.collect { state ->
                when (state) {
                    is States.Success -> {
                        Toast.makeText(requireContext(), "Medicine saved!", Toast.LENGTH_SHORT).show()
                        medicineViewModel.resetAddEditState()
                    }
                    is States.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        medicineViewModel.resetAddEditState()
                    }
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vitalViewModel.addEditState.collect { state ->
                when (state) {
                    is States.Success -> {
                        Toast.makeText(requireContext(), "Vital saved!", Toast.LENGTH_SHORT).show()
                        vitalViewModel.resetAddEditState()
                    }
                    is States.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        vitalViewModel.resetAddEditState()
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}