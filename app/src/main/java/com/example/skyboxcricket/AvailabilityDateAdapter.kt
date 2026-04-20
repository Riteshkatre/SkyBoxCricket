package com.example.skyboxcricket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.skyboxcricket.databinding.ItemAvailabilityDateBinding

class AvailabilityDateAdapter(
    private val onDateClicked: (AvailabilityDateItem) -> Unit
) : RecyclerView.Adapter<AvailabilityDateAdapter.AvailabilityDateViewHolder>() {

    private val items = mutableListOf<AvailabilityDateItem>()

    fun submitList(dateItems: List<AvailabilityDateItem>) {
        items.clear()
        items.addAll(dateItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailabilityDateViewHolder {
        val binding = ItemAvailabilityDateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AvailabilityDateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AvailabilityDateViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AvailabilityDateViewHolder(
        private val binding: ItemAvailabilityDateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AvailabilityDateItem) {
            binding.dayNumberTextView.text = item.dayNumber
            binding.dayLabelTextView.text = item.dayLabel
            binding.monthLabelTextView.text = item.monthLabel

            val backgroundRes = if (item.isSelected) {
                R.drawable.bg_availability_date_selected
            } else {
                R.drawable.bg_availability_date_default
            }
            binding.root.setBackgroundResource(backgroundRes)

            val primaryColor = if (item.isSelected) {
                ContextCompat.getColor(itemView.context, R.color.text_on_dark)
            } else {
                ContextCompat.getColor(itemView.context, R.color.text_primary)
            }
            val secondaryColor = if (item.isSelected) {
                ContextCompat.getColor(itemView.context, R.color.text_on_dark)
            } else {
                ContextCompat.getColor(itemView.context, R.color.text_secondary)
            }

            binding.dayNumberTextView.setTextColor(primaryColor)
            binding.dayLabelTextView.setTextColor(secondaryColor)
            binding.monthLabelTextView.setTextColor(secondaryColor)

            binding.root.setOnClickListener {
                onDateClicked(item)
            }
        }
    }
}
