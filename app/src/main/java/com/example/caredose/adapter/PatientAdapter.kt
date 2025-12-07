package com.example.caredose.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caredose.database.entities.Patient
import com.example.caredose.databinding.ItemPatientBinding

class PatientAdapter(
    private val onPatientClick: (Patient) -> Unit = {},   // click on card (details)
    private val onEditClick: (Patient) -> Unit,
    private val onDeleteClick: (Patient) -> Unit
) : ListAdapter<Patient, PatientAdapter.PatientViewHolder>(PatientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val binding = ItemPatientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PatientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PatientViewHolder(
        private val binding: ItemPatientBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(patient: Patient) {
            binding.apply {
                tvPatientName.text = patient.name
                tvPatientDetails.text = "${patient.age} yrs • ${patient.gender}"

                // Card click -> details / edit or view
                root.setOnClickListener {
                    onPatientClick(patient)
                }

                // Edit button
                btnEdit.setOnClickListener {
                    onEditClick(patient)
                }

                // Delete button
                btnDelete.setOnClickListener {
                    onDeleteClick(patient)
                }
            }
        }
    }

    class PatientDiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean {
            return oldItem.patientId == newItem.patientId
        }

        override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean {
            return oldItem == newItem
        }
    }
}
