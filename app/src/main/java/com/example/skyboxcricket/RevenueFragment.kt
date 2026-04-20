package com.example.skyboxcricket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

    private val repository = BookingRepository()
    private val adapter = BookingListAdapter()
    private var bookingListener: ValueEventListener? = null
    private var monthBookings: List<Booking> = emptyList()
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

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
        binding.revenueRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.revenueRecyclerView.adapter = adapter
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
        binding.revenueProgressBar.visibility = View.VISIBLE
        bookingListener = repository.observeBookings(
            onChanged = ::renderRevenue,
            onError = ::showMessage
        )
    }

    override fun onStop() {
        repository.removeListener(bookingListener)
        bookingListener = null
        super.onStop()
    }

    private fun renderRevenue(bookings: List<Booking>) {
        binding.revenueProgressBar.visibility = View.GONE

        val now = Calendar.getInstance()
        monthBookings = bookings.filter { booking ->
            val bookingCalendar = Calendar.getInstance().apply { timeInMillis = booking.createdAt }
            bookingCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                bookingCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        }

        binding.monthRevenueValueTextView.text =
            currencyFormatter.format(monthBookings.sumOf { it.totalAmount })

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
        binding.revenueProgressBar.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
