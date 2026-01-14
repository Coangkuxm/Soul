package com.example.soul.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.soul.databinding.ActivityMainBinding
import com.example.soul.ui.auth.LoginActivity
import com.example.soul.utils.Resource

/**
 * Main Activity - Home screen after login
 * Uses MVVM pattern with MainViewModel
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is logged in
        if (!viewModel.isLoggedIn()) {
            navigateToLogin()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
        observeViewModel()
    }
    
    /**
     * Setup initial UI state
     */
    private fun setupUI() {
        // Display user info
        binding.tvWelcomeUser.text = viewModel.getWelcomeMessage()
        binding.tvResult.text = viewModel.getUserInfoDisplay()
    }

    /**
     * Setup click listeners
     */
    private fun setupClickListeners() {
        // Test connection button
        binding.btnTest.setOnClickListener {
            viewModel.testConnection()
        }
        
        // Logout button
        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    /**
     * Observe ViewModel LiveData
     */
    private fun observeViewModel() {
        // Observe current user changes
        viewModel.currentUser.observe(this) { user ->
            if (user != null) {
                binding.tvWelcomeUser.text = viewModel.getWelcomeMessage()
                binding.tvResult.text = viewModel.getUserInfoDisplay()
            }
        }
        
        // Observe connection state
        viewModel.connectionState.observe(this) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.tvServerResponse.text = "🔄 Đang kết nối đến server..."
                }
                is Resource.Success -> {
                    val healthResponse = state.data
                    binding.tvServerResponse.text = "✅ Kết nối thành công!\n\n${healthResponse?.getDisplayText()}"
                }
                is Resource.Error -> {
                    binding.tvServerResponse.text = "❌ ${state.message}"
                }
            }
        }
        
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnTest.isEnabled = !isLoading
        }
        
        // Observe logout event
        viewModel.logoutEvent.observe(this) { shouldLogout ->
            if (shouldLogout) {
                viewModel.onLogoutHandled()
                navigateToLogin()
            }
        }
    }
    
    /**
     * Show logout confirmation dialog
     */
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    /**
     * Navigate to login screen
     */
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
