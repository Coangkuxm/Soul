package com.example.soul.ui.auth

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.data.repository.AuthRepository
import com.example.soul.databinding.ActivityForgotPasswordBinding
import com.example.soul.utils.Resource
import kotlinx.coroutines.launch

/**
 * Quên mật khẩu theo 2 bước trong một màn:
 *  1. Nhập email -> backend gửi mã OTP 6 số (test mode: trả mã trong response).
 *  2. Nhập mã + mật khẩu mới -> đặt lại mật khẩu.
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var authRepository: AuthRepository

    private var emailForReset: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authRepository = AuthRepository(
            apiService = RetrofitClient.apiService,
            authPreferences = AuthPreferences(this)
        )

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSendCode.setOnClickListener {
            requestCode(binding.etEmail.text?.toString()?.trim().orEmpty())
        }
        binding.tvResend.setOnClickListener {
            emailForReset?.let { requestCode(it) }
        }
        binding.btnReset.setOnClickListener { submitReset() }
    }

    private fun requestCode(email: String) {
        binding.tilEmail.error = null
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email không hợp lệ"
            return
        }

        lifecycleScope.launch {
            setLoading(true)
            val result = authRepository.forgotPassword(email)
            setLoading(false)

            when (result) {
                is Resource.Success -> {
                    emailForReset = email
                    showStep2()
                    val devCode = result.data?.devCode
                    if (!devCode.isNullOrBlank()) {
                        // Test mode: tự điền mã để tiện kiểm thử
                        binding.etCode.setText(devCode)
                        toast("Mã (test): $devCode")
                    } else {
                        toast("Đã gửi mã đặt lại tới email của bạn")
                    }
                }
                is Resource.Error -> toast(result.message ?: "Không thể gửi mã")
                else -> Unit
            }
        }
    }

    private fun submitReset() {
        val email = emailForReset
        if (email == null) {
            toast("Vui lòng yêu cầu mã trước")
            return
        }

        binding.tilCode.error = null
        binding.tilNewPassword.error = null
        binding.tilConfirmPassword.error = null

        val code = binding.etCode.text?.toString()?.trim().orEmpty()
        val newPassword = binding.etNewPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        var ok = true
        if (code.length != 6) {
            binding.tilCode.error = "Mã gồm 6 chữ số"
            ok = false
        }
        if (newPassword.length < 6) {
            binding.tilNewPassword.error = "Mật khẩu mới tối thiểu 6 ký tự"
            ok = false
        }
        if (newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Mật khẩu nhập lại không khớp"
            ok = false
        }
        if (!ok) return

        lifecycleScope.launch {
            setLoading(true)
            val result = authRepository.resetPassword(email, code, newPassword)
            setLoading(false)

            when (result) {
                is Resource.Success -> {
                    toast("Đặt lại mật khẩu thành công. Hãy đăng nhập lại.")
                    finish()
                }
                is Resource.Error -> toast(result.message ?: "Đặt lại mật khẩu thất bại")
                else -> Unit
            }
        }
    }

    private fun showStep2() {
        binding.layoutStep1.visibility = View.GONE
        binding.layoutStep2.visibility = View.VISIBLE
        binding.tvSubtitle.text = "Nhập mã đã gửi tới $emailForReset và mật khẩu mới"
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSendCode.isEnabled = !isLoading
        binding.btnReset.isEnabled = !isLoading
        binding.tvResend.isEnabled = !isLoading
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
