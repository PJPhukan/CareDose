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
import com.example.caredose.databinding.FragmentVitalListBinding
import com.example.caredose.repository.MasterVitalRepository
import com.example.caredose.viewmodels.MasterVitalViewModel

import com.example.caredose.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch

class VitalListFragment : Fragment() {

    private var _binding: FragmentVitalListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MasterVitalViewModel
    private lateinit var adapter: MasterVitalAdapter
    private lateinit var  sessionManager : SessionManager

    var onItemClick: ((Long, String, String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVitalListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        observeViewModel()

        sessionManager = SessionManager(requireContext())
        val userId = sessionManager.getUserId() as Long
        loadVitals(userId)
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = MasterVitalRepository(database)
        val factory = ViewModelFactory(masterVitalRepository = repository)
        viewModel = ViewModelProvider(this, factory)[MasterVitalViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = MasterVitalAdapter { vital ->
            onItemClick?.invoke(vital.vitalId, vital.name, vital.unit)
        }

        binding.rvVitals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@VitalListFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.vitalsState.collect { state ->
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
                            binding.rvVitals.visibility = View.GONE
                        } else {
                            binding.emptyState.visibility = View.GONE
                            binding.rvVitals.visibility = View.VISIBLE
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

    fun loadVitals(userId: Long) {
        viewModel.loadVitals(userId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}