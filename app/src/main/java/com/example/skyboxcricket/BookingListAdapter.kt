package com.example.skyboxcricket

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.skyboxcricket.databinding.ItemBookingBinding
import java.text.NumberFormat
import java.util.Locale

class BookingListAdapter(
    private val onEdit: ((Booking) -> Unit)? = null,
    private val onDelete: ((Booking) -> Unit)? = null
) : RecyclerView.Adapter<BookingListAdapter.BookingViewHolder>() {

    private val items = mutableListOf<Booking>()
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    fun submitList(bookings: List<Booking>) {
        items.clear()
        items.addAll(bookings)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class BookingViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.customerNameTextView.text = booking.customerName
            binding.bookingMetaTextView.text = itemView.context.getString(
                R.string.booking_meta,
                booking.boxSelection,
                booking.bookingDateTime,
                booking.toDateTime
            )
            binding.paymentModeTextView.text = itemView.context.getString(
                R.string.payment_split,
                currencyFormatter.format(booking.onlineAmount),
                currencyFormatter.format(booking.offlineAmount)
            )
            binding.amountTextView.text = currencyFormatter.format(booking.totalAmount)
            binding.priceSplitTextView.text = itemView.context.getString(
                R.string.price_split,
                currencyFormatter.format(booking.boxPrice),
                currencyFormatter.format(booking.cafePrice)
            )
            val actionsVisible = if (onEdit != null || onDelete != null) android.view.View.VISIBLE else android.view.View.GONE
            binding.actionsContainer.visibility = actionsVisible
            binding.editButton.setOnClickListener { onEdit?.invoke(booking) }
            binding.deleteButton.setOnClickListener { onDelete?.invoke(booking) }
        }
    }
}
