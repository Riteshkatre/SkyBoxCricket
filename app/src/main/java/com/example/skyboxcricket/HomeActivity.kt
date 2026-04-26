package com.example.skyboxcricket

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.skyboxcricket.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            openAuth()
            return
        }

        val isPrivilegedUser = UserAccess.isPrivilegedUser(auth.currentUser?.email)

        setupInsets()
        configureNavigation(isPrivilegedUser)

        binding.topAppBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_logout) {
                auth.signOut()
                openAuth()
                true
            } else {
                false
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    binding.topAppBar.title = getString(R.string.dashboard_title)
                    openFragment(HomeDashboardFragment())
                    true
                }

                R.id.navigation_availability -> {
                    binding.topAppBar.title = getString(R.string.availability_title)
                    openFragment(AvailabilityFragment())
                    true
                }

                R.id.navigation_booking -> {
                    binding.topAppBar.title = getString(R.string.booking_tab_title)
                    openFragment(BookingFragment())
                    true
                }

                R.id.navigation_revenue -> {
                    binding.topAppBar.title = getString(R.string.revenue_tab_title)
                    openFragment(RevenueFragment())
                    true
                }

                else -> false
            }
        }

        if (savedInstanceState == null) {
            binding.bottomNavigationView.selectedItemId =
                if (isPrivilegedUser) R.id.navigation_home else R.id.navigation_booking
        }
    }

    private fun configureNavigation(isPrivilegedUser: Boolean) {
        val menu = binding.bottomNavigationView.menu
        menu.findItem(R.id.navigation_home)?.isVisible = isPrivilegedUser
        menu.findItem(R.id.navigation_revenue)?.isVisible = isPrivilegedUser
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

            binding.bottomNavigationView.setPadding(
                binding.bottomNavigationView.paddingLeft,
                binding.bottomNavigationView.paddingTop,
                binding.bottomNavigationView.paddingRight,
                systemBars.bottom + resources.getDimensionPixelSize(R.dimen.bottom_nav_extra_padding)
            )

            binding.fragmentContainer.setPadding(
                binding.fragmentContainer.paddingLeft,
                binding.fragmentContainer.paddingTop,
                binding.fragmentContainer.paddingRight,
                resources.getDimensionPixelSize(R.dimen.content_bottom_spacing)
            )

            insets
        }
    }

    fun switchToHomeTab() {
        binding.bottomNavigationView.selectedItemId = R.id.navigation_home
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun openAuth() {
        startActivity(Intent(this, AuthActivity::class.java))
        finish()
    }
}
