package com.example.skyboxcricket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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

    private val repository = BookingRepository()
    private var bookingListener: ValueEventListener? = null
    private val dateAdapter = AvailabilityDateAdapter(::onDateSelected)
    private val bookingAdapter = AvailabilityBookingAdapter(
        onEdit = ::editBooking,
        onDelete = ::confirmDeleteBooking
    )

    private var allBookings: List<Booking> = emptyList()
    private var selectedDateMillis: Long = startOfDay(Calendar.getInstance().timeInMillis)
    private var selectedFilter: String = FILTER_BOX_1
    private var rangeStartMillis: Long? = null
    private var rangeEndMillis: Long? = null

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

        binding.dateRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.dateRecyclerView.adapter = dateAdapter

        binding.availabilityRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.availabilityRecyclerView.adapter = bookingAdapter

        binding.box1FilterButton.setOnClickListener { updateFilter(FILTER_BOX_1) }
        binding.box2FilterButton.setOnClickListener { updateFilter(FILTER_BOX_2) }
        binding.bothFilterButton.setOnClickListener { updateFilter(FILTER_BOTH) }
        binding.selectDateFilterButton.setOnClickListener { showDateFilterOptions() }

        updateDateStrip()
        updateFilterButtons()
    }

    override fun onStart() {
        super.onStart()
        binding.availabilityProgressBar.visibility = View.VISIBLE
        bookingListener = repository.observeBookings(
            onChanged = { bookings ->
                allBookings = bookings
                binding.availabilityProgressBar.visibility = View.GONE
                renderAvailability()
            },
            onError = ::showMessage
        )
    }

    override fun onStop() {
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
        rangeStartMillis = null
        rangeEndMillis = null
        binding.selectedDateTextView.text = item.fullDateLabel
        updateDateStrip()
        renderAvailability()
    }

    private fun updateFilter(filter: String) {
        selectedFilter = filter
        updateFilterButtons()
        renderAvailability()
    }

    private fun updateFilterButtons() {
        updateFilterButton(binding.box1FilterButton, selectedFilter == FILTER_BOX_1)
        updateFilterButton(binding.box2FilterButton, selectedFilter == FILTER_BOX_2)
        updateFilterButton(binding.bothFilterButton, selectedFilter == FILTER_BOTH)
    }

    private fun updateFilterButton(button: com.google.android.material.button.MaterialButton, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.brand_blue))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_dark))
        } else {
            button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.surface_card))
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_orange))
        }
    }

    private fun renderAvailability() {
        binding.selectedDateTextView.text = when {
            rangeStartMillis != null && rangeEndMillis != null ->
                BookingDateUtils.formatRangeLabel(rangeStartMillis!!, rangeEndMillis!!)
            else -> BookingDateUtils.formatDateLabel(selectedDateMillis)
        }

        val filteredBookings = allBookings.filter { booking ->
            booking.matchesDateFilter() && booking.matchesFilter(selectedFilter)
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

    private fun Booking.matchesDateFilter(): Boolean {
        val parsedDate = BookingDateUtils.parseDateTime(bookingDateTime)?.time ?: return false
        val bookingDay = BookingDateUtils.startOfDay(parsedDate)
        return if (rangeStartMillis != null && rangeEndMillis != null) {
            bookingDay in rangeStartMillis!!..rangeEndMillis!!
        } else {
            belongsToDate(selectedDateMillis)
        }
    }

    private fun Booking.matchesFilter(filter: String): Boolean {
        return when (filter) {
            FILTER_BOX_1 -> boxSelection.equals("Open Box 1", ignoreCase = true)
            FILTER_BOX_2 -> boxSelection.equals("Open Box 2", ignoreCase = true)
            FILTER_BOTH -> boxSelection.equals("Both", ignoreCase = true)
            else -> true
        }
    }

    private fun startOfDay(timeInMillis: Long): Long {
        return BookingDateUtils.startOfDay(timeInMillis)
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
                        rangeStartMillis = null
                        rangeEndMillis = null
                        selectedDateMillis = startOfDay(System.currentTimeMillis())
                        updateDateStrip()
                        renderAvailability()
                    }
                }
            }
            .show()
    }

    private fun pickSingleDate() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        android.app.DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDateMillis = startOfDay(calendar.timeInMillis)
                rangeStartMillis = null
                rangeEndMillis = null
                updateDateStrip()
                renderAvailability()
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
                rangeStartMillis = startOfDay(calendar.timeInMillis)
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
                rangeEndMillis = startOfDay(calendar.timeInMillis)
                if (rangeStartMillis != null && rangeEndMillis != null && rangeEndMillis!! < rangeStartMillis!!) {
                    val temp = rangeStartMillis
                    rangeStartMillis = rangeEndMillis
                    rangeEndMillis = temp
                }
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
                repository.deleteBooking(
                    bookingId = booking.id,
                    onSuccess = { showMessage(getString(R.string.booking_deleted)) },
                    onError = ::showMessage
                )
            }
            .show()
    }

    private fun showMessage(message: String) {
        if (!isAdded) return
        binding.availabilityProgressBar.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val FILTER_BOX_1 = "box_1"
        private const val FILTER_BOX_2 = "box_2"
        private const val FILTER_BOTH = "both"
    }
}
