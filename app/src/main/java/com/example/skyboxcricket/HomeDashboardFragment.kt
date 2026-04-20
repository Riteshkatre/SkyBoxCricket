package com.example.skyboxcricket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skyboxcricket.databinding.FragmentHomeDashboardBinding
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class HomeDashboardFragment : Fragment() {

    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!

    private val repository = BookingRepository()
    private var bookingListener: ValueEventListener? = null
    private val adapter = BookingListAdapter()
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recentRecyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        binding.homeProgressBar.visibility = View.VISIBLE
        bookingListener = repository.observeBookings(
            onChanged = ::renderBookings,
            onError = ::showMessage
        )
    }

    override fun onStop() {
        repository.removeListener(bookingListener)
        bookingListener = null
        super.onStop()
    }

    private fun renderBookings(bookings: List<Booking>) {
        binding.homeProgressBar.visibility = View.GONE

        val totalRevenue = bookings.sumOf { it.totalAmount }
        val onlineRevenue = bookings.sumOf { it.onlineAmount }
        val offlineRevenue = bookings.sumOf { it.offlineAmount }

        binding.revenueValueTextView.text = currencyFormatter.format(totalRevenue)
        binding.onlineRevenueValueTextView.text = currencyFormatter.format(onlineRevenue)
        binding.offlineRevenueValueTextView.text = currencyFormatter.format(offlineRevenue)
        binding.totalBookingsValueTextView.text =
            getString(R.string.total_bookings_value, bookings.size)

        if (bookings.isEmpty()) {
            binding.emptyStateTextView.visibility = View.VISIBLE
            binding.recentRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateTextView.visibility = View.GONE
            binding.recentRecyclerView.visibility = View.VISIBLE
            adapter.submitList(bookings.take(10))
        }
    }

    private fun showMessage(message: String) {
        if (!isAdded) return
        binding.homeProgressBar.visibility = View.GONE
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
