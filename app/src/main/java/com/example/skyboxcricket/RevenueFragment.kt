package com.example.skyboxcricket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skyboxcricket.databinding.FragmentRevenueBinding
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class RevenueFragment : Fragment() {

    private var _binding: FragmentRevenueBinding? = null
    private val binding get() = _binding!!
    private var loadingDialog: AppLoadingDialog? = null

    private val repository = BookingRepository()
    private val adapter = BookingListAdapter(
        onEdit = ::editBooking,
        onDelete = ::confirmDeleteBooking
    )
    private var bookingListener: ValueEventListener? = null
    private var allBookings: List<Booking> = emptyList()
    private var monthBookings: List<Booking> = emptyList()
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private var rangeStartMillis: Long? = null
    private var rangeEndMillis: Long? = null
    private var selectedDateMillis: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRevenueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = activity?.let(::AppLoadingDialog)
        binding.revenueRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.revenueRecyclerView.adapter = adapter
        binding.selectRevenueDateButton.setOnClickListener { showDateFilterOptions() }
        binding.exportButton.setOnClickListener {
            if (monthBookings.isEmpty()) {
                showMessage(getString(R.string.no_revenue_to_export))
            } else {
                CsvExporter.shareMonthlyRevenue(requireContext(), monthBookings)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        loadingDialog?.show()
        bookingListener = repository.observeBookings(
            onChanged = {
                allBookings = it
                renderRevenue(it)
            },
            onError = ::showMessage
        )
    }

    override fun onStop() {
        loadingDialog?.dismiss()
        repository.removeListener(bookingListener)
        bookingListener = null
        super.onStop()
    }

    private fun renderRevenue(bookings: List<Booking>) {
        loadingDialog?.dismiss()

        monthBookings = bookings.filter { booking ->
            val parsed = BookingDateUtils.parseDateTime(booking.bookingDateTime)?.time ?: booking.createdAt
            val bookingDay = BookingDateUtils.startOfDay(parsed)
            when {
                rangeStartMillis != null && rangeEndMillis != null -> bookingDay in rangeStartMillis!!..rangeEndMillis!!
                selectedDateMillis != null -> bookingDay == selectedDateMillis
                else -> {
                    val now = Calendar.getInstance()
                    val bookingCalendar = Calendar.getInstance().apply { timeInMillis = parsed }
                    bookingCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                        bookingCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
            }
        }

        binding.monthRevenueValueTextView.text =
            currencyFormatter.format(monthBookings.sumOf { it.totalAmount })
        binding.revenueFilterValueTextView.text = when {
            rangeStartMillis != null && rangeEndMillis != null ->
                BookingDateUtils.formatRangeLabel(rangeStartMillis!!, rangeEndMillis!!)
            selectedDateMillis != null ->
                BookingDateUtils.formatDateLabel(selectedDateMillis!!)
            else -> getString(R.string.current_month_filter)
        }

        if (monthBookings.isEmpty()) {
            binding.revenueEmptyStateTextView.visibility = View.VISIBLE
            binding.revenueRecyclerView.visibility = View.GONE
        } else {
            binding.revenueEmptyStateTextView.visibility = View.GONE
            binding.revenueRecyclerView.visibility = View.VISIBLE
            adapter.submitList(monthBookings)
        }
    }

    private fun showMessage(message: String) {
        if (!isAdded) return
        loadingDialog?.dismiss()
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        loadingDialog?.dismiss()
        loadingDialog = null
        _binding = null
        super.onDestroyView()
    }

    private fun showDateFilterOptions() {
        val options = arrayOf(
            getString(R.string.single_date_filter),
            getString(R.string.range_date_filter),
            getString(R.string.clear_date_filter)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.select_date_filter_title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickSingleDate()
                    1 -> pickRangeStartDate()
                    2 -> {
                        selectedDateMillis = null
                        rangeStartMillis = null
                        rangeEndMillis = null
                        renderRevenue(allBookings)
                    }
                }
            }
            .show()
    }

    private fun pickSingleDate() {
        val calendar = Calendar.getInstance()
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDateMillis = BookingDateUtils.startOfDay(calendar.timeInMillis)
                rangeStartMillis = null
                rangeEndMillis = null
                renderRevenue(allBookings)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickRangeStartDate() {
        val calendar = Calendar.getInstance()
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                rangeStartMillis = BookingDateUtils.startOfDay(calendar.timeInMillis)
                pickRangeEndDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickRangeEndDate() {
        val calendar = Calendar.getInstance().apply { timeInMillis = rangeStartMillis ?: System.currentTimeMillis() }
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                rangeEndMillis = BookingDateUtils.startOfDay(calendar.timeInMillis)
                if (rangeStartMillis != null && rangeEndMillis != null && rangeEndMillis!! < rangeStartMillis!!) {
                    val temp = rangeStartMillis
                    rangeStartMillis = rangeEndMillis
                    rangeEndMillis = temp
                }
                selectedDateMillis = null
                renderRevenue(allBookings)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun editBooking(booking: Booking) {
        startActivity(EditBookingActivity.createIntent(requireContext(), booking))
    }

    private fun confirmDeleteBooking(booking: Booking) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_booking_title)
            .setMessage(R.string.delete_booking_message)
            .setNegativeButton(R.string.cancel_text, null)
            .setPositiveButton(R.string.delete_text) { _, _ ->
                loadingDialog?.show()
                repository.deleteBooking(
                    bookingId = booking.id,
                    onSuccess = {
                        loadingDialog?.dismiss()
                        showMessage(getString(R.string.booking_deleted))
                    },
                    onError = { message ->
                        loadingDialog?.dismiss()
                        showMessage(message)
                    }
                )
            }
            .show()
    }
}
