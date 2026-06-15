package com.example.soul.ui.profile

import android.net.Uri
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.User
import com.example.soul.data.model.UserProfile
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityEditProfileBinding
import com.example.soul.ui.main.MainTabsActivity
import com.example.soul.utils.ImagePickerHelper
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class EditProfileActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EditProfileActivity"
        const val EXTRA_ONBOARDING = "extra_onboarding"
    }

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var authPreferences: AuthPreferences

    private var selectedImageUri: Uri? = null
    private var currentAvatarUrl: String? = null
    private var removeAvatar = false
    private var isOnboarding = false

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            removeAvatar = false
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivAvatar)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        isOnboarding = intent.getBooleanExtra(EXTRA_ONBOARDING, false)

        setupToolbar()
        bindCurrentUser()
        setupListeners()
        setupOnboardingUi()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            if (isOnboarding) navigateToMain() else finish()
        }
    }

    private fun setupOnboardingUi() {
        if (!isOnboarding) return
        binding.toolbar.title = "Hoan thien ho so"
        binding.btnSave.text = "Hoan tat"
        binding.btnSkipProfile.visibility = android.view.View.VISIBLE
    }

    private fun bindCurrentUser() {
        val user = authPreferences.getUser() ?: return

        binding.etUsername.setText(user.username)
        binding.etEmail.setText(user.email)
        binding.etDisplayName.setText(user.displayName.orEmpty())
        binding.etBio.setText(user.bio.orEmpty())

        currentAvatarUrl = user.avatarUrl
        if (!currentAvatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(currentAvatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(binding.ivAvatar)
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_default_avatar)
        }
    }

    private fun setupListeners() {
        binding.btnChangeAvatar.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnRemoveAvatar.setOnClickListener {
            selectedImageUri = null
            removeAvatar = true
            currentAvatarUrl = null
            binding.ivAvatar.setImageResource(R.drawable.ic_default_avatar)
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.btnSkipProfile.setOnClickListener {
            navigateToMain()
        }
    }

    private fun saveProfile() {
        val token = authPreferences.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Vui long dang nhap lai", Toast.LENGTH_SHORT).show()
            return
        }

        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val displayName = binding.etDisplayName.text?.toString()?.trim().orEmpty()
        val bio = binding.etBio.text?.toString()?.trim().orEmpty()

        if (username.length !in 3..30 || !username.matches(Regex("^[A-Za-z0-9_]+$"))) {
            binding.tilUsername.error = "Username 3-30 ky tu, chi gom chu, so, dau _"
            return
        }
        binding.tilUsername.error = null

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Email khong hop le"
            return
        }
        binding.tilEmail.error = null

        if (displayName.length > 100) {
            binding.tilDisplayName.error = "Ten hien thi toi da 100 ky tu"
            return
        }
        binding.tilDisplayName.error = null

        if (bio.length > 500) {
            binding.tilBio.error = "Bio toi da 500 ky tu"
            return
        }
        binding.tilBio.error = null

        setLoading(true)

        lifecycleScope.launch {
            try {
                var avatarUrl = currentAvatarUrl
                if (removeAvatar) {
                    avatarUrl = ""
                }
                selectedImageUri?.let {
                    avatarUrl = uploadAvatar(token, it) ?: currentAvatarUrl
                    removeAvatar = false
                }

                val body = mutableMapOf<String, Any?>(
                    "username" to username,
                    "email" to email,
                    "displayName" to displayName,
                    "bio" to bio
                )
                if (avatarUrl != null || removeAvatar) {
                    body["avatarUrl"] = avatarUrl ?: ""
                }

                val response = RetrofitClient.apiService.updateCurrentUser(
                    token = "Bearer $token",
                    body = body
                )

                if (response.isSuccessful && response.body()?.user != null) {
                    val updatedUser = response.body()!!.user!!.toUser()
                    authPreferences.saveUser(updatedUser)
                    currentAvatarUrl = updatedUser.avatarUrl
                    selectedImageUri = null
                    removeAvatar = false
                    Toast.makeText(this@EditProfileActivity, "Cap nhat ho so thanh cong", Toast.LENGTH_SHORT).show()
                    if (isOnboarding) {
                        navigateToMain()
                    } else {
                        setResult(RESULT_OK)
                        finish()
                    }
                } else {
                    val errorMessage = parseError(response.errorBody()?.string(), "Khong the cap nhat ho so")
                    Toast.makeText(this@EditProfileActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating profile", e)
                Toast.makeText(this@EditProfileActivity, e.message ?: "Da xay ra loi", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false)
            }
        }
    }

    private suspend fun uploadAvatar(token: String, uri: Uri): String? {
        return try {
            if (!ImagePickerHelper.isFileSizeValid(this, uri)) {
                Toast.makeText(this, "Anh qua lon (toi da 5MB)", Toast.LENGTH_SHORT).show()
                return null
            }

            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()

            if (bytes == null) return null

            val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", "avatar.jpg", requestBody)
            val folderPart = "avatars".toRequestBody("text/plain".toMediaTypeOrNull())

            val response = RetrofitClient.apiService.uploadImage(
                token = "Bearer $token",
                image = imagePart,
                folder = folderPart
            )

            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!["data"] as? Map<*, *>
                data?.get("url") as? String
            } else {
                Log.e(TAG, "Avatar upload failed: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Avatar upload error", e)
            null
        }
    }

    private fun parseError(raw: String?, fallback: String): String {
        return try {
            val json = JSONObject(raw ?: "")
            json.optString("error").ifEmpty { json.optString("message", fallback) }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSave.isEnabled = !isLoading
        binding.btnChangeAvatar.isEnabled = !isLoading
        binding.btnRemoveAvatar.isEnabled = !isLoading
        binding.btnSkipProfile.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainTabsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun UserProfile.toUser(): User {
        return User(
            id = id,
            username = username,
            email = email,
            displayName = displayName,
            avatarUrl = avatarUrl,
            bio = bio,
            role = role,
            accountStatus = accountStatus,
            followerCount = followerCount,
            followingCount = followingCount,
            collectionCount = collectionCount
        )
    }
}
