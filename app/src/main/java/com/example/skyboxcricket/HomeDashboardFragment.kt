package com.example.skyboxcricket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.skyboxcricket.databinding.FragmentHomeDashboardBinding
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class HomeDashboardFragment : Fragment() {

    private var _binding: FragmentHomeDashboardBinding? = null
    private val binding get() = _binding!!

    private val repository = BookingRepository()
    private var bookingListener: ValueEventListener? = null
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
        val boxRevenue = bookings.sumOf { it.boxPrice }
        val cafeRevenue = bookings.sumOf { it.cafePrice }

        binding.revenueValueTextView.text = currencyFormatter.format(totalRevenue)
        binding.onlineRevenueValueTextView.text = currencyFormatter.format(onlineRevenue)
        binding.offlineRevenueValueTextView.text = currencyFormatter.format(offlineRevenue)
        binding.totalBookingsValueTextView.text =
            getString(R.string.total_bookings_value, bookings.size)
        binding.chartCenterValueTextView.text = currencyFormatter.format(totalRevenue)
        binding.chartCenterLabelTextView.text =
            if (bookings.isEmpty()) getString(R.string.no_data_label) else getString(R.string.total_revenue_title)
        binding.revenuePieChartView.setData(totalRevenue, onlineRevenue, offlineRevenue)
        binding.boxCafePieChartView.setCustomPairData(
            firstValue = boxRevenue,
            secondValue = cafeRevenue,
            firstColor = ContextCompat.getColor(requireContext(), R.color.brand_orange),
            secondColor = ContextCompat.getColor(requireContext(), R.color.brand_blue)
        )
        binding.boxCafeCenterValueTextView.text = currencyFormatter.format(boxRevenue + cafeRevenue)
        binding.boxCafeStatBoxValueTextView.text = currencyFormatter.format(boxRevenue)
        binding.boxCafeStatCafeValueTextView.text = currencyFormatter.format(cafeRevenue)
        binding.statOnlineValueTextView.text = currencyFormatter.format(onlineRevenue)
        binding.statOfflineValueTextView.text = currencyFormatter.format(offlineRevenue)
        binding.statTotalValueTextView.text = currencyFormatter.format(totalRevenue)
        binding.statBookingsValueTextView.text = bookings.size.toString()

        if (bookings.isEmpty()) {
            binding.emptyStateTextView.visibility = View.VISIBLE
        } else {
            binding.emptyStateTextView.visibility = View.GONE
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
