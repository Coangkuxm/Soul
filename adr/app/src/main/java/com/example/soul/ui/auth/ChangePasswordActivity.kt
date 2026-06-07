package com.example.soul.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.data.repository.AuthRepository
import com.example.soul.databinding.ActivityChangePasswordBinding
import kotlinx.coroutines.launch

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var authRepository: AuthRepository
    private lateinit var authPreferences: AuthPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        authRepository = AuthRepository(
            apiService = RetrofitClient.apiService,
            authPreferences = authPreferences
        )

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSave.setOnClickListener { submit() }
    }

    private fun submit() {
        clearErrors()
        val currentPassword = binding.etCurrentPassword.text?.toString().orEmpty()
        val newPassword = binding.etNewPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        if (!validate(currentPassword, newPassword, confirmPassword)) return

        lifecycleScope.launch {
            setLoading(true)
            val result = authRepository.changePassword(currentPassword, newPassword)
            setLoading(false)

            if (result.data?.success == true) {
                Toast.makeText(this@ChangePasswordActivity, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(
                    this@ChangePasswordActivity,
                    result.message ?: "Không thể đổi mật khẩu",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validate(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
        var ok = true
        if (currentPassword.length < 6) {
            binding.tilCurrentPassword.error = "Vui lòng nhập mật khẩu hiện tại"
            ok = false
        }
        if (newPassword.length < 6) {
            binding.tilNewPassword.error = "Mật khẩu mới phải có ít nhất 6 ký tự"
            ok = false
        }
        if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Mật khẩu nhập lại không khớp"
            ok = false
        }
        if (currentPassword == newPassword) {
            binding.tilNewPassword.error = "Mật khẩu mới phải khác mật khẩu cũ"
            ok = false
        }
        return ok
    }

    private fun clearErrors() {
        binding.tilCurrentPassword.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
    }
}
