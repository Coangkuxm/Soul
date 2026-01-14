package com.example.soul.ui.add

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityAddCollectionBinding
import kotlinx.coroutines.launch
import org.json.JSONObject

class AddCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCollectionBinding
    private lateinit var authPreferences: AuthPreferences
    private var isPrivate = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCollectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)

        setupToolbar()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        // Privacy switch
        binding.switchPrivate.setOnCheckedChangeListener { _, isChecked ->
            isPrivate = isChecked
            updatePrivacyUI()
        }

        // Cover image picker
        binding.cardCover.setOnClickListener {
            // TODO: Implement image picker
            Toast.makeText(this, "Chọn ảnh bìa - sẽ cập nhật sau", Toast.LENGTH_SHORT).show()
        }

        // Create button
        binding.btnCreate.setOnClickListener {
            createCollection()
        }
    }

    private fun updatePrivacyUI() {
        if (isPrivate) {
            binding.ivPrivacyIcon.setImageResource(R.drawable.ic_private)
            binding.tvPrivacyTitle.text = "Collection riêng tư"
            binding.tvPrivacyDesc.text = "Chỉ bạn có thể xem collection này"
        } else {
            binding.ivPrivacyIcon.setImageResource(R.drawable.ic_public)
            binding.tvPrivacyTitle.text = "Collection công khai"
            binding.tvPrivacyDesc.text = "Mọi người có thể xem collection này"
        }
    }

    private fun createCollection() {
        val name = binding.etName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        // Validate
        if (name.isEmpty()) {
            binding.tilName.error = "Vui lòng nhập tên collection"
            return
        }
        binding.tilName.error = null

        showLoading(true)

        lifecycleScope.launch {
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@AddCollectionActivity, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }

                val requestBody = mapOf(
                    "name" to name,
                    "description" to description,
                    "is_private" to isPrivate
                )

                val response = RetrofitClient.apiService.createCollection(
                    token = "Bearer $token",
                    body = requestBody
                )

                if (response.isSuccessful) {
                    Toast.makeText(this@AddCollectionActivity, "Tạo collection thành công!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        JSONObject(errorBody ?: "").optString("message", "Lỗi tạo collection")
                    } catch (e: Exception) {
                        "Lỗi tạo collection"
                    }
                    Toast.makeText(this@AddCollectionActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddCollectionActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnCreate.isEnabled = !show
    }
}
