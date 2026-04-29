package com.example.skyboxcricket

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.skyboxcricket.databinding.DialogEditBookingBinding
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

        val paymentSplitHelper = PaymentSplitAutoFillHelper(
            totalAmountEditText = binding.totalAmountEditText,
            onlineAmountEditText = binding.onlineAmountEditText,
            offlineAmountEditText = binding.offlineAmountEditText
        ).also { it.attach() }

        val totalWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val boxPrice = binding.boxPriceEditText.text.toString().toDoubleOrNull() ?: 0.0
                val cafePrice = binding.cafePriceEditText.text.toString().toDoubleOrNull() ?: 0.0
                binding.totalAmountEditText.setText((boxPrice + cafePrice).toString())
            }
        }
        binding.boxPriceEditText.addTextChangedListener(totalWatcher)
        binding.cafePriceEditText.addTextChangedListener(totalWatcher)

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

                if (!PaymentSplitValidator.isValid(updated.totalAmount, updated.onlineAmount, updated.offlineAmount)) {
                    val remainingAmount = PaymentSplitValidator.formatAmount(
                        PaymentSplitValidator.getRemainingAmount(
                            updated.totalAmount,
                            updated.onlineAmount,
                            updated.offlineAmount
                        )
                    )
                    Toast.makeText(
                        context,
                        context.getString(R.string.payment_split_remaining_error, remainingAmount),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                onSave(updated)
                paymentSplitHelper.detach()
                dialog.dismiss()
            }
        }

        dialog.setOnDismissListener {
            paymentSplitHelper.detach()
            binding.boxPriceEditText.removeTextChangedListener(totalWatcher)
            binding.cafePriceEditText.removeTextChangedListener(totalWatcher)
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
