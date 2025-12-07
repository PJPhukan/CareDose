package com.example.caredose.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caredose.database.AppDatabase
import com.example.caredose.database.entities.MedicineStock
import com.example.caredose.databinding.ItemMedicineStockBinding
import kotlinx.coroutines.launch

class MedicineStockAdapter(
    private val onEditClick: (MedicineStock) -> Unit,
    private val onDeleteClick: (MedicineStock) -> Unit,
    private val onIncrementStock: (MedicineStock) -> Unit,
    private val onDecrementStock: (MedicineStock) -> Unit
) : ListAdapter<MedicineStock, MedicineStockAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMedicineStockBinding.inflate(
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
        private val binding: ItemMedicineStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: MedicineStock) {
            val context = binding.root.context
            val lifecycleOwner = context as? LifecycleOwner

            lifecycleOwner?.lifecycleScope?.launch {
                val db = AppDatabase.getDatabase(context)
                val medicine = db.masterMedicineDao().getById(stock.masterMedicineId)

                binding.apply {
                    tvMedicineName.text = medicine?.name ?: "Unknown Medicine"
                    tvStockQty.text = "Stock: ${stock.stockQty} Tablet"

                    // Low stock warning
                    if (stock.stockQty <= stock.reminderStockThreshold) {
                        tvLowStock.visibility = android.view.View.VISIBLE
                        tvLowStock.text = "⚠️ Low Stock"
                    } else {
                        tvLowStock.visibility = android.view.View.GONE
                    }

                    // Buttons
                    btnEdit.setOnClickListener { onEditClick(stock) }
                    btnDelete.setOnClickListener { onDeleteClick(stock) }
                    btnIncrement.setOnClickListener { onIncrementStock(stock) }
                    btnDecrement.setOnClickListener { onDecrementStock(stock) }

                    // Card click
                    root.setOnClickListener { onEditClick(stock) }
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MedicineStock>() {
        override fun areItemsTheSame(oldItem: MedicineStock, newItem: MedicineStock): Boolean {
            return oldItem.stockId == newItem.stockId
        }

        override fun areContentsTheSame(oldItem: MedicineStock, newItem: MedicineStock): Boolean {
            return oldItem == newItem
        }
    }
}