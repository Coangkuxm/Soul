package com.example.soul.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.data.repository.AuthRepository
import com.example.soul.databinding.ActivityRegisterBinding
import com.example.soul.ui.profile.EditProfileActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(
            apiService = RetrofitClient.apiService,
            authPreferences = AuthPreferences(this)
        )

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRegister.setOnClickListener { submit() }
        binding.tvGoToLogin.setOnClickListener { finish() }
    }

    private fun submit() {
        clearErrors()
        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val displayName = binding.etDisplayName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

        if (!validate(username, email, password, confirm)) return

        lifecycleScope.launch {
            setLoading(true)
            val result = authRepository.register(
                username = username,
                email = email,
                password = password,
                displayName = displayName
            )
            setLoading(false)

            if (result.data?.success == true) {
                Toast.makeText(this@RegisterActivity, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RegisterActivity, EditProfileActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(EditProfileActivity.EXTRA_ONBOARDING, true)
                })
                finish()
            } else {
                Toast.makeText(
                    this@RegisterActivity,
                    result.message ?: "Đăng ký thất bại",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validate(username: String, email: String, password: String, confirm: String): Boolean {
        var ok = true
        if (username.length < 3) {
            binding.tilUsername.error = "Tên đăng nhập phải có ít nhất 3 ký tự"
            ok = false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email không hợp lệ"
            ok = false
        }
        if (!isStrongPassword(password)) {
            binding.tilPassword.error = "Mật khẩu: 8+ ký tự, gồm hoa/thường/số/ký tự đặc biệt"
            ok = false
        }
        if (password != confirm) {
            binding.tilConfirmPassword.error = "Mật khẩu nhập lại không khớp"
            ok = false
        }
        return ok
    }

    private fun isStrongPassword(password: String): Boolean {
        val regex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$")
        return regex.matches(password)
    }

    private fun clearErrors() {
        binding.tilUsername.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }
}
