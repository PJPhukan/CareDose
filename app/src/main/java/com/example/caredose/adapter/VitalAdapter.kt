package com.example.caredose.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.Vital
import com.example.caredose.databinding.ItemVitalBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VitalAdapter(
    private val onEditClick: (Vital) -> Unit,
    private val onDeleteClick: (Vital) -> Unit
) : ListAdapter<Vital, VitalAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVitalBinding.inflate(
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
        private val binding: ItemVitalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(vital: Vital) {
            val context = binding.root.context
            val lifecycleOwner = context as? LifecycleOwner

            lifecycleOwner?.lifecycleScope?.launch {
                val db = AppDatabase.getDatabase(context)
                val vitalType = db.masterVitalDao().getById(vital.masterVitalId)

                binding.apply {
                    tvVitalType.text = vitalType?.name ?: "Unknown Vital"
                    tvVitalValue.text = "${vital.value} ${vitalType?.unit ?: ""}"
                    tvVitalDate.text = formatDate(vital.recordedAt)
                    tvVitalNote.text = vital.note ?: ""

                    if (vital.note.isNullOrEmpty()) {
                        tvVitalNote.visibility = android.view.View.GONE
                    } else {
                        tvVitalNote.visibility = android.view.View.VISIBLE
                    }

                    btnEdit.setOnClickListener { onEditClick(vital) }
                    btnDelete.setOnClickListener { onDeleteClick(vital) }
                    root.setOnClickListener { onEditClick(vital) }
                }
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Vital>() {
        override fun areItemsTheSame(oldItem: Vital, newItem: Vital): Boolean {
            return oldItem.vitalRecordId == newItem.vitalRecordId
        }

        override fun areContentsTheSame(oldItem: Vital, newItem: Vital): Boolean {
            return oldItem == newItem
        }
    }
}