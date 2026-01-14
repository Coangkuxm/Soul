package com.example.soul.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.soul.databinding.ActivityLoginBinding
import com.example.soul.ui.home.HomeActivity
import com.example.soul.utils.Resource

/**
 * Login Activity - handles user authentication
 * Uses MVVM pattern with LoginViewModel
 */
class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    
    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if already logged in
        if (viewModel.isLoggedIn()) {
            navigateToMain()
            return
        }
        
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    /**
     * Setup click listeners for UI elements
     */
    private fun setupClickListeners() {
        // Login button click
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.login(email, password)
        }

        // Forgot password click
        binding.tvForgotPassword.setOnClickListener {
            showToast("Tính năng quên mật khẩu sẽ được cập nhật sau")
        }

        // Social login buttons
        binding.btnGoogle.setOnClickListener {
            showToast("Đăng nhập bằng Google sẽ được cập nhật sau")
        }

        binding.btnFacebook.setOnClickListener {
            showToast("Đăng nhập bằng Facebook sẽ được cập nhật sau")
        }

        binding.btnApple.setOnClickListener {
            showToast("Đăng nhập bằng Apple sẽ được cập nhật sau")
        }
    }

    /**
     * Observe ViewModel LiveData
     */
    private fun observeViewModel() {
        // Observe login state
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is Resource.Loading -> {
                    // Loading handled by isLoading LiveData
                }
                is Resource.Success -> {
                    val user = state.data?.user
                    val displayName = user?.displayName ?: user?.username ?: "User"
                    showToast("Đăng nhập thành công! Xin chào $displayName")
                    navigateToMain()
                }
                is Resource.Error -> {
                    showToast(state.message ?: "Đăng nhập thất bại")
                }
            }
        }
        
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }
        
        // Observe validation errors
        viewModel.emailError.observe(this) { error ->
            binding.tilEmail.error = error
        }
        
        viewModel.passwordError.observe(this) { error ->
            binding.tilPassword.error = error
        }
    }
    
    /**
     * Show/hide loading state
     */
    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
        binding.btnFacebook.isEnabled = !isLoading
        binding.btnApple.isEnabled = !isLoading
    }

    /**
     * Navigate to Main screen
     */
    private fun navigateToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
