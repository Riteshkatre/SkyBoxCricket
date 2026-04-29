package com.example.skyboxcricket

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.skyboxcricket.databinding.FragmentBookingBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
class BookingFragment : Fragment() {

    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!
    private var loadingDialog: AppLoadingDialog? = null

    private val repository = BookingRepository()
    private val bookingCalendar = Calendar.getInstance()
    private val toCalendar = Calendar.getInstance()
    private var paymentSplitHelper: PaymentSplitAutoFillHelper? = null

    private val amountWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            updateTotalAmount()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = activity?.let(::AppLoadingDialog)
        setupDropdown()
        setupPriceWatchers()
        paymentSplitHelper = PaymentSplitAutoFillHelper(
            totalAmountEditText = binding.totalAmountEditText,
            onlineAmountEditText = binding.onlineAmountEditText,
            offlineAmountEditText = binding.offlineAmountEditText
        ).also { it.attach() }
        binding.bookingDateTimeEditText.setOnClickListener {
            pickDateTime(
                calendar = bookingCalendar,
                target = { value -> binding.bookingDateTimeEditText.setText(value) }
            )
        }
        binding.toDateTimeEditText.setOnClickListener {
            pickDateTime(
                calendar = toCalendar,
                target = { value -> binding.toDateTimeEditText.setText(value) }
            )
        }
        binding.submitBookingButton.setOnClickListener { submitBooking() }
    }

    private fun setupDropdown() {
        val options = resources.getStringArray(R.array.box_selection_options)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        binding.boxSelectionAutoComplete.setAdapter(adapter)
    }

    private fun setupPriceWatchers() {
        binding.boxPriceEditText.addTextChangedListener(amountWatcher)
        binding.cafePriceEditText.addTextChangedListener(amountWatcher)
    }

    private fun pickDateTime(
        calendar: Calendar,
        target: (String) -> Unit
    ) {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        target(
                            SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                .format(calendar.time)
                        )
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateTotalAmount() {
        val boxPrice = binding.boxPriceEditText.text.toString().toDoubleOrNull() ?: 0.0
        val cafePrice = binding.cafePriceEditText.text.toString().toDoubleOrNull() ?: 0.0
        binding.totalAmountEditText.setText((boxPrice + cafePrice).toString())
    }

    private fun submitBooking() {
        val customerName = binding.nameEditText.text.toString().trim()
        val boxSelection = binding.boxSelectionAutoComplete.text.toString().trim()
        val bookingDateTime = binding.bookingDateTimeEditText.text.toString().trim()
        val toDateTime = binding.toDateTimeEditText.text.toString().trim()
        val boxPrice = binding.boxPriceEditText.text.toString().toDoubleOrNull()
        val cafePrice = binding.cafePriceEditText.text.toString().toDoubleOrNull()
        val totalAmount = binding.totalAmountEditText.text.toString().toDoubleOrNull()
        val onlineAmount = binding.onlineAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val offlineAmount = binding.offlineAmountEditText.text.toString().toDoubleOrNull() ?: 0.0

        if (
            customerName.isEmpty() ||
            boxSelection.isEmpty() ||
            bookingDateTime.isEmpty() ||
            toDateTime.isEmpty() ||
            boxPrice == null ||
            cafePrice == null ||
            totalAmount == null
        ) {
            showMessage(getString(R.string.fill_booking_fields))
            return
        }

        if (!PaymentSplitValidator.isValid(totalAmount, onlineAmount, offlineAmount)) {
            val remainingAmount = PaymentSplitValidator.formatAmount(
                PaymentSplitValidator.getRemainingAmount(totalAmount, onlineAmount, offlineAmount)
            )
            showMessage(getString(R.string.payment_split_remaining_error, remainingAmount))
            return
        }

        setLoading(true)
        repository.addBooking(
            booking = Booking(
                customerName = customerName,
                boxSelection = boxSelection,
                bookingDateTime = bookingDateTime,
                toDateTime = toDateTime,
                boxPrice = boxPrice,
                cafePrice = cafePrice,
                totalAmount = totalAmount,
                onlineAmount = onlineAmount,
                offlineAmount = offlineAmount,
                createdAt = System.currentTimeMillis()
            ),
            onSuccess = {
                if (!isAdded) return@addBooking
                setLoading(false)
                showMessage(getString(R.string.booking_saved))
                clearForm()
                (activity as? HomeActivity)?.switchToHomeTab()
            },
            onError = { message ->
                if (!isAdded) return@addBooking
                setLoading(false)
                showMessage(message)
            }
        )
    }

    private fun clearForm() {
        binding.nameEditText.text?.clear()
        binding.boxSelectionAutoComplete.text?.clear()
        binding.bookingDateTimeEditText.text?.clear()
        binding.toDateTimeEditText.text?.clear()
        binding.boxPriceEditText.text?.clear()
        binding.cafePriceEditText.text?.clear()
        binding.totalAmountEditText.text?.clear()
        binding.onlineAmountEditText.setText("0")
        binding.offlineAmountEditText.setText("0")
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingDialog?.show()
        } else {
            loadingDialog?.dismiss()
        }
        binding.submitBookingButton.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        loadingDialog?.dismiss()
        loadingDialog = null
        binding.boxPriceEditText.removeTextChangedListener(amountWatcher)
        binding.cafePriceEditText.removeTextChangedListener(amountWatcher)
        paymentSplitHelper?.detach()
        paymentSplitHelper = null
        _binding = null
        super.onDestroyView()
    }
}
