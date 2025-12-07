package com.example.caredose.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.caredose.SessionManager
import com.example.caredose.States
import com.example.caredose.database.AppDatabase
import com.example.caredose.databinding.FragmentMedicineListBinding
import com.example.caredose.repository.MasterMedicineRepository
import com.example.caredose.viewmodels.MasterMedicineViewModel

import com.example.caredose.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch

class MedicineListFragment : Fragment() {

    private var _binding: FragmentMedicineListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MasterMedicineViewModel
    private lateinit var adapter: MasterMedicineAdapter
    private lateinit var  sessionManager : SessionManager
    var onItemClick: ((Long, String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicineListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        observeViewModel()

         sessionManager = SessionManager(requireContext())
        val userId = sessionManager.getUserId() as Long
        loadMedicines(userId)
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = MasterMedicineRepository(database)
        val factory = ViewModelFactory(masterMedicineRepository = repository)
        viewModel = ViewModelProvider(this, factory)[MasterMedicineViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = MasterMedicineAdapter { medicine ->
            onItemClick?.invoke(medicine.medicineId, medicine.name)
        }

        binding.rvMedicines.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MedicineListFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.medicinesState.collect { state ->
                when (state) {
                    is States.Idle -> {
                        binding.progressBar.visibility = View.GONE
                    }
                    is States.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.emptyState.visibility = View.GONE
                    }
                    is States.Success -> {
                        binding.progressBar.visibility = View.GONE
                        if (state.data.isEmpty()) {
                            binding.emptyState.visibility = View.VISIBLE
                            binding.rvMedicines.visibility = View.GONE
                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.rvMedicines.visibility = View.VISIBLE
                            adapter.submitList(state.data)
                        }
                    }
                    is States.Error -> {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        }
    }

    fun loadMedicines(userId: Long) {
        viewModel.loadMedicines(userId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}