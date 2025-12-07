package com.example.caredose.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caredose.database.entities.MasterMedicine
import com.example.caredose.databinding.ItemMasterMedicineBinding

class MasterMedicineAdapter(
    private val onItemClick: (MasterMedicine) -> Unit
) : ListAdapter<MasterMedicine, MasterMedicineAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMasterMedicineBinding.inflate(
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
        private val binding: ItemMasterMedicineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(medicine: MasterMedicine) {
            binding.tvMedicineName.text = medicine.name
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MasterMedicine>() {
        override fun areItemsTheSame(oldItem: MasterMedicine, newItem: MasterMedicine): Boolean {
            return oldItem.medicineId == newItem.medicineId
        }

        override fun areContentsTheSame(oldItem: MasterMedicine, newItem: MasterMedicine): Boolean {
            return oldItem == newItem
        }
    }
}