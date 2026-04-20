package com.example.skyboxcricket

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.skyboxcricket.databinding.ActivityAuthBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            openHome()
            return
        }

        setupTabs()
        setupClickListeners()
    }

    private fun setupTabs() {
        updateTabContent(binding.authTabs.selectedTabPosition)

        binding.authTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabContent(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun setupClickListeners() {
        binding.createAccountButton.setOnClickListener {
            createAccount()
        }

        binding.signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun updateTabContent(position: Int) {
        binding.signUpContainer.visibility = if (position == 0) View.VISIBLE else View.GONE
        binding.signInContainer.visibility = if (position == 1) View.VISIBLE else View.GONE
    }

    private fun createAccount() {
        val email = binding.signUpEmailEditText.text.toString().trim()
        val password = binding.signUpPasswordEditText.text.toString().trim()
        val confirmPassword = binding.signUpConfirmPasswordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage(getString(R.string.fill_all_fields))
            return
        }

        if (password != confirmPassword) {
            showMessage(getString(R.string.password_mismatch))
            return
        }

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    showMessage(getString(R.string.account_created))
                    openHome()
                } else {
                    showMessage(task.exception?.localizedMessage ?: getString(R.string.auth_failed))
                }
            }
    }

    private fun signIn() {
        val email = binding.signInEmailEditText.text.toString().trim()
        val password = binding.signInPasswordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showMessage(getString(R.string.fill_all_fields))
            return
        }

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    showMessage(getString(R.string.login_success))
                    openHome()
                } else {
                    showMessage(task.exception?.localizedMessage ?: getString(R.string.auth_failed))
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.authProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.createAccountButton.isEnabled = !isLoading
        binding.signInButton.isEnabled = !isLoading
    }

    private fun openHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
