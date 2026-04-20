package com.example.skyboxcricket

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.skyboxcricket.databinding.DialogEditBookingBinding
import kotlin.math.abs

object BookingEditDialog {

    fun show(
        fragment: Fragment,
        booking: Booking,
        onSave: (Booking) -> Unit
    ) {
        val context = fragment.requireContext()
        val binding = DialogEditBookingBinding.inflate(LayoutInflater.from(context))
        val bookingCalendar = java.util.Calendar.getInstance()
        val toCalendar = java.util.Calendar.getInstance()

        val options = context.resources.getStringArray(R.array.box_selection_options)
        binding.boxSelectionAutoComplete.setAdapter(
            ArrayAdapter(context, android.R.layout.simple_list_item_1, options)
        )

        binding.nameEditText.setText(booking.customerName)
        binding.boxSelectionAutoComplete.setText(booking.boxSelection, false)
        binding.bookingDateTimeEditText.setText(booking.bookingDateTime)
        binding.toDateTimeEditText.setText(booking.toDateTime)
        binding.boxPriceEditText.setText(booking.boxPrice.toString())
        binding.cafePriceEditText.setText(booking.cafePrice.toString())
        binding.totalAmountEditText.setText(booking.totalAmount.toString())
        binding.onlineAmountEditText.setText(booking.onlineAmount.toString())
        binding.offlineAmountEditText.setText(booking.offlineAmount.toString())

        binding.bookingDateTimeEditText.setOnClickListener {
            pickDateTime(context, bookingCalendar) { value ->
                binding.bookingDateTimeEditText.setText(value)
            }
        }
        binding.toDateTimeEditText.setOnClickListener {
            pickDateTime(context, toCalendar) { value ->
                binding.toDateTimeEditText.setText(value)
            }
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.edit_booking_title)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel_text, null)
            .setPositiveButton(R.string.save_changes, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val updated = booking.copy(
                    customerName = binding.nameEditText.text.toString().trim(),
                    boxSelection = binding.boxSelectionAutoComplete.text.toString().trim(),
                    bookingDateTime = binding.bookingDateTimeEditText.text.toString().trim(),
                    toDateTime = binding.toDateTimeEditText.text.toString().trim(),
                    boxPrice = binding.boxPriceEditText.text.toString().toDoubleOrNull() ?: 0.0,
                    cafePrice = binding.cafePriceEditText.text.toString().toDoubleOrNull() ?: 0.0,
                    totalAmount = binding.totalAmountEditText.text.toString().toDoubleOrNull() ?: 0.0,
                    onlineAmount = binding.onlineAmountEditText.text.toString().toDoubleOrNull() ?: 0.0,
                    offlineAmount = binding.offlineAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
                )

                if (
                    updated.customerName.isBlank() ||
                    updated.boxSelection.isBlank() ||
                    updated.bookingDateTime.isBlank() ||
                    updated.toDateTime.isBlank()
                ) {
                    Toast.makeText(context, R.string.fill_booking_fields, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (abs((updated.onlineAmount + updated.offlineAmount) - updated.totalAmount) > 0.01) {
                    Toast.makeText(context, R.string.payment_split_error, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                onSave(updated)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun pickDateTime(
        context: android.content.Context,
        calendar: java.util.Calendar,
        onSelected: (String) -> Unit
    ) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(java.util.Calendar.YEAR, year)
                calendar.set(java.util.Calendar.MONTH, month)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(java.util.Calendar.MINUTE, minute)
                        onSelected(
                            java.text.SimpleDateFormat(
                                "dd MMM yyyy, hh:mm a",
                                java.util.Locale.getDefault()
                            ).format(calendar.time)
                        )
                    },
                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                    calendar.get(java.util.Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).show()
    }
}
