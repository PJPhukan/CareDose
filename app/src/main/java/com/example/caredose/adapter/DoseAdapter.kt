package com.example.caredose.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Dose
import com.example.caredose.databinding.ItemDoseBinding
import kotlinx.coroutines.launch

class DoseAdapter(
    private val onEditClick: (Dose) -> Unit,
    private val onDeleteClick: (Dose) -> Unit,
    private val onMarkTakenClick: (Dose) -> Unit
) : ListAdapter<Dose, DoseAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDoseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDoseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dose: Dose) {
            val context = binding.root.context
            val lifecycleOwner = context as? LifecycleOwner

            lifecycleOwner?.lifecycleScope?.launch {
                val db = AppDatabase.getDatabase(context)
                val stock = db.medicineStockDao().getById(dose.stockId)
                val medicine = stock?.let { db.masterMedicineDao().getById(it.masterMedicineId) }

                binding.apply {
                    tvMedicineName.text = medicine?.name ?: "Unknown Medicine"
                    tvDoseTime.text = formatTime(dose.timeInMinutes)
                    tvDoseQuantity.text = "${dose.quantity} Tablet"
                    tvStatus.text = if (dose.isActive) "Active" else "Inactive"
                    tvStatus.setTextColor(
                        if (dose.isActive)
                            android.graphics.Color.parseColor("#4CAF50")
                        else
                            android.graphics.Color.parseColor("#F44336")
                    )

                    if (dose.isTakenToday) {
                        binding.btnMarkTaken.text = "✓ Taken"
                        binding.btnMarkTaken.isEnabled = false // Disable after taking
                        // Optional: change color to gray
                        binding.btnMarkTaken.alpha = 0.5f
                    } else {
                        binding.btnMarkTaken.text = "Mark Taken"
                        binding.btnMarkTaken.isEnabled = dose.isActive // Only enable if the dose is active
                        binding.btnMarkTaken.alpha = 1.0f
                    }

                    btnEdit.setOnClickListener { onEditClick(dose) }
                    btnDelete.setOnClickListener { onDeleteClick(dose) }
                    root.setOnClickListener { onEditClick(dose) }

                    binding.btnMarkTaken.setOnClickListener {
                        // Check if enabled (prevent double click race condition)
                        if (binding.btnMarkTaken.isEnabled) {
                            // Optimistically disable the button immediately
                            binding.btnMarkTaken.isEnabled = false
                            onMarkTakenClick(dose)
                        }
                    }
                }
            }
        }

        private fun formatTime(timeInMinutes: Int): String {
            val hour = timeInMinutes / 60
            val minute = timeInMinutes % 60
            return String.format(
                "%02d:%02d %s",
                if (hour % 12 == 0) 12 else hour % 12,
                minute,
                if (hour < 12) "AM" else "PM"
            )
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Dose>() {
        override fun areItemsTheSame(oldItem: Dose, newItem: Dose): Boolean {
            return oldItem.doseId == newItem.doseId
        }

        override fun areContentsTheSame(oldItem: Dose, newItem: Dose): Boolean {
            return oldItem == newItem
        }
    }
}