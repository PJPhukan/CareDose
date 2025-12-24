package com.example.caredose.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Dose
import com.example.caredose.database.entities.DurationType
import com.example.caredose.databinding.ItemDoseBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DoseAdapter(
    private val onEditClick: (Dose) -> Unit,
    private val onDeleteClick: (Dose) -> Unit,
    private val onMarkTakenClick: (Dose) -> Unit
) : ListAdapter<Dose, DoseAdapter.DoseViewHolder>(DoseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoseViewHolder {
        val binding = ItemDoseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DoseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DoseViewHolder(
        private val binding: ItemDoseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dose: Dose) {
            binding.tvMedicineName.text = "Loading..."

                      CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(binding.root.context)
                    val medicine = db.masterMedicineDao().getById(dose.medicineId)
                    val medicineName = medicine?.name ?: "Unknown Medicine"

                    withContext(Dispatchers.Main) {
                        binding.tvMedicineName.text = medicineName
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.tvMedicineName.text = "Unknown Medicine"
                    }
                }
            }
            binding.tvDoseTime.text = formatTime(dose.timeInMinutes)

            binding.tvDoseQuantity.text = "${dose.quantity} tablet${if (dose.quantity > 1) "s" else ""}"

            val statusText = buildString {
                if (!dose.isActive) {
                    append("Inactive")
                } else if (dose.isExpired()) {
                    append("Expired")
                } else {
                    append("Active")

                    if (dose.endDate != null) {
                        append(" • ")
                        append(getDurationText(dose))
                    } else {
                        append(" • Continuous")
                    }
                }
            }

            binding.tvStatus.text = statusText

            binding.tvStatus.setTextColor(
                when {
                    !dose.isActive -> Color.GRAY
                    dose.isExpired() -> Color.RED
                    dose.endDate != null && getRemainingDays(dose.endDate) <= 3 -> Color.parseColor("#FF6F00")
                    else -> Color.parseColor("#4CAF50")
                }
            )

            binding.btnMarkTaken.isEnabled = dose.isActive && !dose.isTakenToday && !dose.isExpired()
            binding.btnMarkTaken.text = if (dose.isTakenToday) "✓ Taken" else "Mark Taken"
            binding.btnMarkTaken.alpha = if (binding.btnMarkTaken.isEnabled) 1.0f else 0.5f

            binding.btnEdit.setOnClickListener { onEditClick(dose) }
            binding.btnDelete.setOnClickListener { onDeleteClick(dose) }
            binding.btnMarkTaken.setOnClickListener { onMarkTakenClick(dose) }
        }

        private fun formatTime(timeInMinutes: Int): String {
            val hour = timeInMinutes / 60
            val minute = timeInMinutes % 60
            val period = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format("%02d:%02d %s", displayHour, minute, period)
        }

        private fun getDurationText(dose: Dose): String {
            val endDate = dose.endDate ?: return "Continuous"
            val remainingDays = getRemainingDays(endDate)

            return when {
                remainingDays < 0 -> "Expired"
                remainingDays == 0L -> "Ends today"
                remainingDays == 1L -> "1 day left"
                remainingDays <= 7 -> "$remainingDays days left"
                remainingDays <= 30 -> {
                    val weeks = remainingDays / 7
                    if (weeks == 1L) "1 week left" else "$weeks weeks left"
                }
                else -> {
                    val months = remainingDays / 30
                    if (months == 1L) "1 month left" else "$months months left"
                }
            }
        }

        private fun getRemainingDays(endDate: Long): Long {
            val now = System.currentTimeMillis()
            return TimeUnit.MILLISECONDS.toDays(endDate - now)
        }
    }

    private class DoseDiffCallback : DiffUtil.ItemCallback<Dose>() {
        override fun areItemsTheSame(oldItem: Dose, newItem: Dose): Boolean {
            return oldItem.doseId == newItem.doseId
        }

        override fun areContentsTheSame(oldItem: Dose, newItem: Dose): Boolean {
            return oldItem == newItem
        }
    }
}