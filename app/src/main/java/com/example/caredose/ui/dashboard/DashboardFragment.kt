package com.example.caredose.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.caredose.R
import com.example.caredose.SessionManager
import com.example.caredose.States
import com.example.caredose.database.AppDatabase
import com.example.caredose.databinding.FragmentDashboardBinding
import com.example.caredose.repository.MasterVitalRepository
import com.example.caredose.repository.PatientRepository
import com.example.caredose.repository.VitalRepository
import com.example.caredose.viewmodels.ViewModelFactory
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel
    private lateinit var sessionManager: SessionManager
    private var dateRangeDays = 7 // Default 7 days

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        setupViewModel()
        setupUI()
        observeViewModel()
    }

    private fun setupViewModel() {
        val db = AppDatabase.getDatabase(requireContext())
        val factory = ViewModelFactory(
            patientRepository = PatientRepository(db),
            vitalRepository = VitalRepository(db.vitalDao()),
            masterVitalRepository = MasterVitalRepository(db)
        )

        viewModel = ViewModelProvider(this, factory)[DashboardViewModel::class.java]

        // Set userId to load user-specific patients
        val userId = sessionManager.getUserId()
        if (userId != -1L) {
            viewModel.setUserId(userId as Long)
        }
    }

    private fun setupUI() {
        // Patient selection
        binding.actvPatient.setOnItemClickListener { _, _, position, _ ->
            val patients = viewModel.patients.value ?: return@setOnItemClickListener
            if (position < patients.size) {
                viewModel.selectPatient(patients[position].patientId)
            }
        }

        // Vital type selection
        binding.actvVitalType.setOnItemClickListener { _, _, position, _ ->
            val vitalTypes = viewModel.vitalTypes.value ?: return@setOnItemClickListener
            if (position < vitalTypes.size) {
                viewModel.selectVitalType(vitalTypes[position].vitalId)
                binding.cardDateRange.visibility = View.VISIBLE

                viewModel.loadVitalsData(dateRangeDays)
            }
        }

        // Date range filter
        // FIX 1: Set the default chip selection visually
        binding.chip7Days.isChecked = true

        binding.chipGroupDateRange.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: -1

            dateRangeDays = when (checkedId) {
                R.id.chip7Days -> 7
                R.id.chip30Days -> 30
                R.id.chip90Days -> 90
                R.id.chipAllTime -> 365 * 10
                else -> 7
            }

            if (viewModel.selectedVitalType.value != null) {
                viewModel.loadVitalsData(dateRangeDays)
            }
        }
    }

    private fun observeViewModel() {
        // Observe patients
        viewModel.patients.observe(viewLifecycleOwner) { patients ->
            if (patients.isEmpty()) {
                showEmptyState()
                return@observe
            }

            val patientNames = patients.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                patientNames
            )
            binding.actvPatient.setAdapter(adapter)
        }

        // Observe vital types
        viewModel.vitalTypes.observe(viewLifecycleOwner) { vitalTypes ->
            if (vitalTypes.isEmpty()) {
                return@observe
            }

            binding.cardVitalSelection.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE

            val vitalTypeNames = vitalTypes.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                vitalTypeNames
            )
            binding.actvVitalType.setAdapter(adapter)
        }

        // Observe vitals data
        viewModel.vitalsData.observe(viewLifecycleOwner) { vitals ->
            if (vitals.isNotEmpty()) {
                displayGraph(vitals)
            }
        }

        // Observe stats
        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.tvMinValue.text = String.format("%.1f", it.min)
                binding.tvAvgValue.text = String.format("%.1f", it.avg)
                binding.tvMaxValue.text = String.format("%.1f", it.max)
            }
        }

        // Observe state (UPDATED for States sealed class)
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is States.Idle -> {
                    binding.progressBar.visibility = View.GONE
                }
                is States.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is States.Success -> {
                    binding.progressBar.visibility = View.GONE
                }
                is States.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun displayGraph(vitals: List<com.example.caredose.database.entities.Vital>) {
        binding.cardGraph.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        // FIX 1: Prepare data for chart using INDEX as the X-value (0, 1, 2, ...)
        val entries = vitals.mapIndexed { index, vital ->
            Entry(index.toFloat(), vital.value.toFloat()) // Use index for X-axis position
        }

        // Configure dataset
        val dataSet = LineDataSet(entries, "Vital Readings").apply {
            color = ContextCompat.getColor(requireContext(), R.color.purple_500)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.purple_500))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.purple_200)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(true)
        }

        val lineData = LineData(dataSet)

        // Configure chart
        binding.lineChart.apply {
            data = lineData
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            // X-Axis - Format dates
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)

                // FIX 2: ValueFormatter correctly uses the index (value) to retrieve the date
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index >= 0 && index < vitals.size) {
                            // Date is looked up using the index from the vitals list
                            val date = Date(vitals[index].recordedAt)
                            SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
                        } else {
                            ""
                        }
                    }
                }
                // FIX 3: Remove conflicting granularity to allow setLabelCount to control all labels
                // granularity = 1f
                labelRotationAngle = -45f
                setLabelCount(vitals.size, true) // Ensure labels match data points
            }

            // Y-Axis - Left
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
            }

            // Y-Axis - Right (disabled)
            axisRight.isEnabled = false

            // Refresh chart
            invalidate()
        }
    }
    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
        binding.cardGraph.visibility = View.GONE
        binding.cardVitalSelection.visibility = View.GONE
        binding.cardDateRange.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}