package com.example.soul.ui.add

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityAddCollectionBinding
import com.example.soul.utils.ImagePickerHelper
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AddCollectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCollectionBinding
    private lateinit var authPreferences: AuthPreferences
    private var isPrivate = false
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null

    // Image picker launcher
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            // Show selected image
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(binding.ivCover)
            binding.ivCameraIcon.visibility = View.GONE
        }
    }

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
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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
                
                // Upload image first if selected
                if (selectedImageUri != null && uploadedImageUrl == null) {
                    uploadedImageUrl = uploadImage(token, selectedImageUri!!)
                }

                val requestBody = mutableMapOf<String, Any?>(
                    "name" to name,
                    "description" to description,
                    "is_private" to isPrivate
                )
                
                // Add cover image URL if uploaded
                uploadedImageUrl?.let { requestBody["cover_image_url"] = it }

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
    
    private suspend fun uploadImage(token: String, uri: Uri): String? {
        return try {
            // Check file size
            if (!ImagePickerHelper.isFileSizeValid(this, uri)) {
                Toast.makeText(this, "Ảnh quá lớn (tối đa 5MB)", Toast.LENGTH_SHORT).show()
                return null
            }
            
            // Read file bytes
            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            
            if (bytes == null) return null
            
            // Create multipart body
            val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", "cover.jpg", requestBody)
            val folderPart = "collections".toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = RetrofitClient.apiService.uploadImage(
                token = "Bearer $token",
                image = imagePart,
                folder = folderPart
            )
            
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                data["data"]?.let { dataMap ->
                    (dataMap as? Map<*, *>)?.get("url") as? String
                }
            } else {
                Toast.makeText(this, "Lỗi upload ảnh", Toast.LENGTH_SHORT).show()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi upload: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}
