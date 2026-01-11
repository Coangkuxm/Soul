package com.example.soul.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.soul.R
import com.example.soul.databinding.ActivityLoginBinding
import com.example.soul.ui.MainActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Login button click
        binding.btnLogin.setOnClickListener {
            if (validateInputs()) {
                // TODO: Implement login logic
                navigateToMain()
            }
        }

        // Forgot password click
        binding.tvForgotPassword.setOnClickListener {
            showToast("Tính năng quên mật khẩu sẽ được cập nhật sau")
        }

        // Social login buttons
        binding.btnGoogle.setOnClickListener {
            showToast("Đăng nhập bằng Google")
        }

        binding.btnFacebook.setOnClickListener {
            showToast("Đăng nhập bằng Facebook")
        }

        binding.btnApple.setOnClickListener {
            showToast("Đăng nhập bằng Apple")
        }
    }

    private fun validateInputs(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_field_required)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_invalid_email)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_field_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_short_password)
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        return isValid
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
