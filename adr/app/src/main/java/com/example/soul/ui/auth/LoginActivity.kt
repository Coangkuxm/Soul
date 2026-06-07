package com.example.soul.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.soul.databinding.ActivityLoginBinding
import com.example.soul.ui.main.MainTabsActivity
import com.example.soul.utils.Resource
import com.example.soul.utils.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.isLoggedIn()) {
            navigateToMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
        showLogoutReasonIfNeeded()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.login(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            showToast("Tính năng quên mật khẩu sẽ được cập nhật sau")
        }

        binding.btnGoogle.setOnClickListener {
            showToast("Đăng nhập Google sẽ được cập nhật sau")
        }

        binding.btnFacebook.setOnClickListener {
            showToast("Đăng nhập Facebook sẽ được cập nhật sau")
        }

        binding.btnApple.setOnClickListener {
            showToast("Đăng nhập Apple sẽ được cập nhật sau")
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is Resource.Loading -> Unit
                is Resource.Success -> {
                    val user = state.data?.user
                    val displayName = user?.displayName ?: user?.username ?: "User"
                    showToast("Đăng nhập thành công. Xin chào $displayName")
                    navigateToMain()
                }
                is Resource.Error -> {
                    showToast(state.message ?: "Đăng nhập thất bại")
                }
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.emailError.observe(this) { error ->
            binding.tilEmail.error = error
        }

        viewModel.passwordError.observe(this) { error ->
            binding.tilPassword.error = error
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
        binding.btnFacebook.isEnabled = !isLoading
        binding.btnApple.isEnabled = !isLoading
        binding.tvSignUp.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainTabsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLogoutReasonIfNeeded() {
        when (intent.getStringExtra(SessionManager.EXTRA_LOGOUT_REASON)) {
            SessionManager.REASON_ACCOUNT_LOCKED -> {
                AlertDialog.Builder(this)
                    .setTitle("Tài khoản đã bị khóa")
                    .setMessage("Phiên đăng nhập đã bị hủy vì tài khoản của bạn hiện đang bị khóa.")
                    .setPositiveButton("Đã hiểu", null)
                    .show()
            }
            SessionManager.REASON_UNAUTHORIZED -> {
                showToast("Phiên đăng nhập đã hết hạn")
            }
        }
        intent.removeExtra(SessionManager.EXTRA_LOGOUT_REASON)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
