package com.example.skyboxcricket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skyboxcricket.databinding.FragmentAvailabilityBinding
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AvailabilityFragment : Fragment() {

    private var _binding: FragmentAvailabilityBinding? = null
    private val binding get() = _binding!!
    private var loadingDialog: AppLoadingDialog? = null

    private val repository = BookingRepository()
    private var bookingListener: ValueEventListener? = null
    private val dateAdapter = AvailabilityDateAdapter(::onDateSelected)
    private val bookingAdapter = AvailabilityBookingAdapter(
        onEdit = ::editBooking,
        onDelete = ::confirmDeleteBooking
    )

    private var allBookings: List<Booking> = emptyList()
    private var selectedDateMillis: Long = startOfDay(Calendar.getInstance().timeInMillis)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAvailabilityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = activity?.let(::AppLoadingDialog)

        binding.dateRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.dateRecyclerView.adapter = dateAdapter

        binding.availabilityRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.availabilityRecyclerView.adapter = bookingAdapter

        binding.selectDateFilterButton.setOnClickListener { pickSingleDate() }

        updateDateStrip()
    }

    override fun onStart() {
        super.onStart()
        loadingDialog?.show()
        bookingListener = repository.observeBookings(
            onChanged = { bookings ->
                allBookings = bookings
                loadingDialog?.dismiss()
                renderAvailability()
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

    private fun updateDateStrip() {
        val baseCalendar = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
        }
        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = startOfDay(baseCalendar.timeInMillis)
            add(Calendar.DAY_OF_MONTH, -1)
        }

        val dateItems = (0 until 7).map { offset ->
            val itemCalendar = (startCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, offset)
            }
            AvailabilityDateItem(
                dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(itemCalendar.time),
                dayNumber = SimpleDateFormat("dd", Locale.getDefault()).format(itemCalendar.time),
                monthLabel = SimpleDateFormat("MMM", Locale.getDefault()).format(itemCalendar.time),
                fullDateLabel = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(itemCalendar.time),
                timestamp = startOfDay(itemCalendar.timeInMillis),
                isSelected = startOfDay(itemCalendar.timeInMillis) == selectedDateMillis
            )
        }
        dateAdapter.submitList(dateItems)
        val selectedIndex = dateItems.indexOfFirst { it.isSelected }.coerceAtLeast(0)
        binding.dateRecyclerView.scrollToPosition(selectedIndex)
    }

    private fun onDateSelected(item: AvailabilityDateItem) {
        selectedDateMillis = item.timestamp
        binding.selectedDateTextView.text = item.fullDateLabel
        updateDateStrip()
        renderAvailability()
    }

    private fun renderAvailability() {
        binding.selectedDateTextView.text = BookingDateUtils.formatDateLabel(selectedDateMillis)

        val filteredBookings = allBookings.filter { booking ->
            booking.belongsToDate(selectedDateMillis)
        }

        if (filteredBookings.isEmpty()) {
            binding.emptyAvailabilityTextView.visibility = View.VISIBLE
            binding.availabilityRecyclerView.visibility = View.GONE
        } else {
            binding.emptyAvailabilityTextView.visibility = View.GONE
            binding.availabilityRecyclerView.visibility = View.VISIBLE
            bookingAdapter.submitList(filteredBookings)
        }
    }

    private fun Booking.belongsToDate(targetDayMillis: Long): Boolean {
        val parsedDate = BookingDateUtils.parseDateTime(bookingDateTime)?.time ?: return false
        return BookingDateUtils.startOfDay(parsedDate) == targetDayMillis
    }

    private fun startOfDay(timeInMillis: Long): Long {
        return BookingDateUtils.startOfDay(timeInMillis)
    }

    private fun pickSingleDate() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDateMillis = startOfDay(calendar.timeInMillis)
                updateDateStrip()
                renderAvailability()
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
}
