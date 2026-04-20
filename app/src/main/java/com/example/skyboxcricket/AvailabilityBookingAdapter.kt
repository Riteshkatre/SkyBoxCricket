package com.example.skyboxcricket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skyboxcricket.databinding.ItemAvailabilityBookingBinding

class AvailabilityBookingAdapter(
    private val onEdit: (Booking) -> Unit,
    private val onDelete: (Booking) -> Unit
) : RecyclerView.Adapter<AvailabilityBookingAdapter.AvailabilityBookingViewHolder>() {

    private val items = mutableListOf<Booking>()

    fun submitList(bookings: List<Booking>) {
        items.clear()
        items.addAll(bookings)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvailabilityBookingViewHolder {
        val binding = ItemAvailabilityBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AvailabilityBookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AvailabilityBookingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AvailabilityBookingViewHolder(
        private val binding: ItemAvailabilityBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.customerNameTextView.text = booking.customerName
            binding.slotTimeTextView.text = itemView.context.getString(
                R.string.availability_time_range,
                BookingDateUtils.extractTime(booking.bookingDateTime),
                BookingDateUtils.extractTime(booking.toDateTime)
            )
            binding.boxMetaTextView.text = itemView.context.getString(
                R.string.availability_box_meta,
                booking.boxSelection
            )
            binding.editButton.setOnClickListener { onEdit(booking) }
            binding.deleteButton.setOnClickListener { onDelete(booking) }
        }
    }
}
