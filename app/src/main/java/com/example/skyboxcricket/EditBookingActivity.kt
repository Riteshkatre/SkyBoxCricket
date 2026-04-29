package com.example.skyboxcricket

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.skyboxcricket.databinding.ActivityEditBookingBinding
class EditBookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBookingBinding
    private lateinit var loadingDialog: AppLoadingDialog
    private lateinit var paymentSplitHelper: PaymentSplitAutoFillHelper
    private val repository = BookingRepository()
    private val bookingCalendar = java.util.Calendar.getInstance()
    private val toCalendar = java.util.Calendar.getInstance()

    private val amountWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(s: Editable?) {
            updateTotalAmount()
        }
    }

    private val bookingId: String by lazy { intent.getStringExtra(EXTRA_BOOKING_ID).orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditBookingBinding.inflate(layoutInflater)
        loadingDialog = AppLoadingDialog(this)
        setContentView(binding.root)

        setupInsets()
        setupToolbar()
        setupForm()
        populateForm()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(
                binding.appBarLayout.paddingLeft,
                systemBars.top,
                binding.appBarLayout.paddingRight,
                binding.appBarLayout.paddingBottom
            )
            binding.formContent.root.setPadding(
                binding.formContent.root.paddingLeft,
                binding.formContent.root.paddingTop,
                binding.formContent.root.paddingRight,
                systemBars.bottom
            )
            insets
        }
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener { finish() }
    }

    private fun setupForm() = with(binding.formContent) {
        bookingTitleTextView.text = getString(R.string.edit_booking_title)
        bookingSubtitleTextView.text = getString(R.string.edit_booking_subtitle)
        submitBookingButton.text = getString(R.string.save_changes)

        val options = resources.getStringArray(R.array.box_selection_options)
        boxSelectionAutoComplete.setAdapter(
            ArrayAdapter(this@EditBookingActivity, android.R.layout.simple_list_item_1, options)
        )

        boxPriceEditText.addTextChangedListener(amountWatcher)
        cafePriceEditText.addTextChangedListener(amountWatcher)
        paymentSplitHelper = PaymentSplitAutoFillHelper(
            totalAmountEditText = totalAmountEditText,
            onlineAmountEditText = onlineAmountEditText,
            offlineAmountEditText = offlineAmountEditText
        ).also { it.attach() }

        bookingDateTimeEditText.setOnClickListener {
            pickDateTime(bookingCalendar) { value -> bookingDateTimeEditText.setText(value) }
        }
        toDateTimeEditText.setOnClickListener {
            pickDateTime(toCalendar) { value -> toDateTimeEditText.setText(value) }
        }
        submitBookingButton.setOnClickListener { updateBooking() }
    }

    private fun populateForm() = with(binding.formContent) {
        nameEditText.setText(intent.getStringExtra(EXTRA_CUSTOMER_NAME).orEmpty())
        boxSelectionAutoComplete.setText(intent.getStringExtra(EXTRA_BOX_SELECTION).orEmpty(), false)
        bookingDateTimeEditText.setText(intent.getStringExtra(EXTRA_BOOKING_DATE_TIME).orEmpty())
        toDateTimeEditText.setText(intent.getStringExtra(EXTRA_TO_DATE_TIME).orEmpty())
        boxPriceEditText.setText(intent.getDoubleExtra(EXTRA_BOX_PRICE, 0.0).toString())
        cafePriceEditText.setText(intent.getDoubleExtra(EXTRA_CAFE_PRICE, 0.0).toString())
        totalAmountEditText.setText(intent.getDoubleExtra(EXTRA_TOTAL_AMOUNT, 0.0).toString())
        onlineAmountEditText.setText(intent.getDoubleExtra(EXTRA_ONLINE_AMOUNT, 0.0).toString())
        offlineAmountEditText.setText(intent.getDoubleExtra(EXTRA_OFFLINE_AMOUNT, 0.0).toString())
    }

    private fun pickDateTime(
        calendar: java.util.Calendar,
        target: (String) -> Unit
    ) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(java.util.Calendar.YEAR, year)
                calendar.set(java.util.Calendar.MONTH, month)
                calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(java.util.Calendar.MINUTE, minute)
                        target(
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

    private fun updateTotalAmount() = with(binding.formContent) {
        val boxPrice = boxPriceEditText.text.toString().toDoubleOrNull() ?: 0.0
        val cafePrice = cafePriceEditText.text.toString().toDoubleOrNull() ?: 0.0
        totalAmountEditText.setText((boxPrice + cafePrice).toString())
    }

    private fun updateBooking() = with(binding.formContent) {
        val customerName = nameEditText.text.toString().trim()
        val boxSelection = boxSelectionAutoComplete.text.toString().trim()
        val bookingDateTime = bookingDateTimeEditText.text.toString().trim()
        val toDateTime = toDateTimeEditText.text.toString().trim()
        val boxPrice = boxPriceEditText.text.toString().toDoubleOrNull()
        val cafePrice = cafePriceEditText.text.toString().toDoubleOrNull()
        val totalAmount = totalAmountEditText.text.toString().toDoubleOrNull()
        val onlineAmount = onlineAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val offlineAmount = offlineAmountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val createdAt = intent.getLongExtra(EXTRA_CREATED_AT, 0L)

        if (
            bookingId.isBlank() ||
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
        repository.updateBooking(
            booking = Booking(
                id = bookingId,
                customerName = customerName,
                boxSelection = boxSelection,
                bookingDateTime = bookingDateTime,
                toDateTime = toDateTime,
                boxPrice = boxPrice,
                cafePrice = cafePrice,
                totalAmount = totalAmount,
                onlineAmount = onlineAmount,
                offlineAmount = offlineAmount,
                createdAt = createdAt
            ),
            onSuccess = {
                setLoading(false)
                showMessage(getString(R.string.booking_updated))
                setResult(RESULT_OK)
                finish()
            },
            onError = { message ->
                setLoading(false)
                showMessage(message)
            }
        )
    }

    private fun setLoading(isLoading: Boolean) = with(binding.formContent) {
        if (isLoading) {
            loadingDialog.show()
        } else {
            loadingDialog.dismiss()
        }
        submitBookingButton.isEnabled = !isLoading
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        loadingDialog.dismiss()
        binding.formContent.boxPriceEditText.removeTextChangedListener(amountWatcher)
        binding.formContent.cafePriceEditText.removeTextChangedListener(amountWatcher)
        paymentSplitHelper.detach()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_BOOKING_ID = "extra_booking_id"
        private const val EXTRA_CUSTOMER_NAME = "extra_customer_name"
        private const val EXTRA_BOX_SELECTION = "extra_box_selection"
        private const val EXTRA_BOOKING_DATE_TIME = "extra_booking_date_time"
        private const val EXTRA_TO_DATE_TIME = "extra_to_date_time"
        private const val EXTRA_BOX_PRICE = "extra_box_price"
        private const val EXTRA_CAFE_PRICE = "extra_cafe_price"
        private const val EXTRA_TOTAL_AMOUNT = "extra_total_amount"
        private const val EXTRA_ONLINE_AMOUNT = "extra_online_amount"
        private const val EXTRA_OFFLINE_AMOUNT = "extra_offline_amount"
        private const val EXTRA_CREATED_AT = "extra_created_at"

        fun createIntent(context: Context, booking: Booking): Intent {
            return Intent(context, EditBookingActivity::class.java)
                .putExtra(EXTRA_BOOKING_ID, booking.id)
                .putExtra(EXTRA_CUSTOMER_NAME, booking.customerName)
                .putExtra(EXTRA_BOX_SELECTION, booking.boxSelection)
                .putExtra(EXTRA_BOOKING_DATE_TIME, booking.bookingDateTime)
                .putExtra(EXTRA_TO_DATE_TIME, booking.toDateTime)
                .putExtra(EXTRA_BOX_PRICE, booking.boxPrice)
                .putExtra(EXTRA_CAFE_PRICE, booking.cafePrice)
                .putExtra(EXTRA_TOTAL_AMOUNT, booking.totalAmount)
                .putExtra(EXTRA_ONLINE_AMOUNT, booking.onlineAmount)
                .putExtra(EXTRA_OFFLINE_AMOUNT, booking.offlineAmount)
                .putExtra(EXTRA_CREATED_AT, booking.createdAt)
        }
    }
}
