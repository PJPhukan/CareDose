package com.example.caredose.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caredose.database.entities.MasterVital
import com.example.caredose.databinding.ItemMasterVitalBinding

class MasterVitalAdapter(
    private val onItemClick: (MasterVital) -> Unit
) : ListAdapter<MasterVital, MasterVitalAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMasterVitalBinding.inflate(
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
        private val binding: ItemMasterVitalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(vital: MasterVital) {
            binding.tvVitalName.text = vital.name
            binding.tvVitalUnit.text = vital.unit
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MasterVital>() {
        override fun areItemsTheSame(oldItem: MasterVital, newItem: MasterVital): Boolean {
            return oldItem.vitalId == newItem.vitalId
        }

        override fun areContentsTheSame(oldItem: MasterVital, newItem: MasterVital): Boolean {
            return oldItem == newItem
        }
    }
}