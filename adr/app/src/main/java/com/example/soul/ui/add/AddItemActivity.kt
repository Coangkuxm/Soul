package com.example.soul.ui.add

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.Collection
import com.example.soul.data.model.SearchResult
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityAddItemBinding
import com.example.soul.utils.ImagePickerHelper
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AddItemActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddItemActivity"
    }

    private lateinit var binding: ActivityAddItemBinding
    private lateinit var authPreferences: AuthPreferences

    private var selectedType = "music"
    private var collections: List<Collection> = emptyList()
    private var selectedCollectionId: Int? = null
    
    // Store selected search result
    private var selectedSearchResult: SearchResult? = null
    private var selectedCoverUrl: String? = null
    
    // For manual image selection
    private var selectedImageUri: Uri? = null
    
    // Image picker launcher
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedCoverUrl = null // Clear URL since we're using local image
            selectedSearchResult = null // Clear search result
            // Show selected image
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(binding.ivCover)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)

        setupToolbar()
        setupTypeChips()
        setupListeners()
        loadCollections()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupTypeChips() {
        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val newType = when (checkedIds[0]) {
                    R.id.chipMusic -> "music"
                    R.id.chipMovie -> "movie"
                    R.id.chipBook -> "book"
                    R.id.chipGame -> "game"
                    R.id.chipArtist -> "artist"
                    R.id.chipOther -> "other"
                    else -> "music"
                }
                
                // Clear search result if type changed
                if (newType != selectedType) {
                    selectedSearchResult = null
                    clearForm()
                }
                
                selectedType = newType
                updateMetadataFields()
            }
        }
        // Initial state
        updateMetadataFields()
    }
    
    private fun clearForm() {
        binding.etTitle.text?.clear()
        binding.etDescription.text?.clear()
        binding.etArtist.text?.clear()
        binding.etAlbum.text?.clear()
        binding.etDirector.text?.clear()
        binding.etAuthor.text?.clear()
        binding.ivCover.setImageResource(R.drawable.bg_placeholder_cover)
        selectedCoverUrl = null
        selectedImageUri = null
        selectedSearchResult = null
    }

    private fun updateMetadataFields() {
        // Hide all optional fields first
        binding.tilArtist.visibility = View.GONE
        binding.tilAlbum.visibility = View.GONE
        binding.tilDirector.visibility = View.GONE
        binding.tilAuthor.visibility = View.GONE
        
        // Show/hide search button based on type
        val needsSearch = selectedType == "music" || selectedType == "movie"
        binding.btnSearch.visibility = if (needsSearch) View.VISIBLE else View.GONE
        
        // For music/movie: hide content until item selected from search
        // For other types: always show content
        val hasSelection = selectedSearchResult != null
        binding.layoutContent.visibility = if (needsSearch && !hasSelection) {
            View.GONE
        } else {
            View.VISIBLE
        }
        
        // Update search button text based on type
        when (selectedType) {
            "music" -> binding.btnSearch.text = "🔍 Tìm kiếm từ Spotify"
            "movie" -> binding.btnSearch.text = "🔍 Tìm kiếm từ TMDB"
        }

        // Show relevant fields based on type
        when (selectedType) {
            "music" -> {
                binding.tilArtist.visibility = View.VISIBLE
                binding.tilArtist.hint = "Nghệ sĩ"
                binding.tilAlbum.visibility = View.VISIBLE
                
                // For music: Disable all fields until song is selected from Spotify
                setMusicFieldsEnabled(hasSelection)
            }
            "movie" -> {
                binding.tilDirector.visibility = View.VISIBLE
            }
            "book" -> {
                binding.tilAuthor.visibility = View.VISIBLE
            }
            "artist" -> {
                // No additional fields
            }
            "game" -> {
                binding.tilDirector.visibility = View.VISIBLE
                binding.tilDirector.hint = "Nhà phát triển"
            }
        }
    }
    
    private fun setMusicFieldsEnabled(enabled: Boolean) {
        // Disable/enable input fields for music type
        binding.etTitle.isEnabled = enabled
        binding.etDescription.isEnabled = enabled
        binding.etArtist.isEnabled = false // Always disabled - auto-filled from Spotify
        binding.etAlbum.isEnabled = false // Always disabled - auto-filled from Spotify
        
        // Update hint to guide user
        if (!enabled && selectedType == "music") {
            binding.tilTitle.hint = "Tên bài hát (tìm kiếm từ Spotify)"
        } else {
            binding.tilTitle.hint = "Tên *"
        }
    }

    private fun setupListeners() {
        // Cover image picker - only allow for types WITHOUT API search (book, game, artist, other)
        binding.cardCover.setOnClickListener {
            // If already selected from API (Spotify/TMDB), don't allow changing
            if (selectedSearchResult != null) {
                Toast.makeText(this, "Ảnh được lấy từ ${if (selectedType == "music") "Spotify" else "TMDB"}", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Only allow manual image selection for non-API types
            if (selectedType != "music" && selectedType != "movie") {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                Toast.makeText(this, "Hãy tìm kiếm từ ${if (selectedType == "music") "Spotify" else "TMDB"} để lấy ảnh", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Search button (for music/movie types)
        binding.btnSearch.setOnClickListener {
            showSearchBottomSheet()
        }

        // Collection dropdown
        binding.actvCollection.setOnItemClickListener { _, _, position, _ ->
            if (position < collections.size) {
                selectedCollectionId = collections[position].id
            }
        }

        // Add button
        binding.btnAdd.setOnClickListener {
            addItem()
        }
    }
    
    private fun showSearchBottomSheet() {
        val mediaType = if (selectedType == "music") "music" else "movie"
        val bottomSheet = SearchMediaBottomSheet.newInstance(mediaType) { result ->
            fillFormFromSearchResult(result)
        }
        bottomSheet.show(supportFragmentManager, SearchMediaBottomSheet.TAG)
    }
    
    private fun fillFormFromSearchResult(result: SearchResult) {
        selectedSearchResult = result
        selectedCoverUrl = result.coverUrl
        selectedImageUri = null // Clear any manual image selection
        
        // Show content container now that item is selected
        binding.layoutContent.visibility = View.VISIBLE
        
        // Fill title
        binding.etTitle.setText(result.title)
        
        // Fill description if available
        result.metadata?.get("subtitle")?.let {
            binding.etDescription.setText(it.toString())
        }
        
        // Load cover image from API - this is locked, user cannot change
        result.coverUrl?.let { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.bg_placeholder_cover)
                .error(R.drawable.bg_placeholder_cover)
                .centerCrop()
                .into(binding.ivCover)
        }
        
        // Fill type-specific fields
        when (selectedType) {
            "music" -> {
                // Fill artist from subtitle (contains artist names)
                result.subtitle?.let { binding.etArtist.setText(it) }
                // Fill album from metadata
                result.metadata?.get("album")?.let { binding.etAlbum.setText(it.toString()) }
                // Enable description field now that song is selected
                setMusicFieldsEnabled(true)
            }
            "movie" -> {
                // Nothing extra to fill for movie
            }
        }
        
        Toast.makeText(this, "Đã chọn: ${result.title}", Toast.LENGTH_SHORT).show()
    }

    private fun loadCollections() {
        lifecycleScope.launch {
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) return@launch

                val response = RetrofitClient.apiService.getCollections(
                    token = "Bearer $token",
                    userId = authPreferences.getUser()?.id,
                    limit = 50
                )

                if (response.isSuccessful && response.body() != null) {
                    collections = response.body()!!.data
                    val collectionNames = collections.map { it.name }
                    val adapter = ArrayAdapter(
                        this@AddItemActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        collectionNames
                    )
                    binding.actvCollection.setAdapter(adapter)

                    // Auto select first collection if available
                    if (collections.isNotEmpty()) {
                        binding.actvCollection.setText(collections[0].name, false)
                        selectedCollectionId = collections[0].id
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading collections", e)
            }
        }
    }

    private fun addItem() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val rating = binding.ratingBar.rating.toInt().takeIf { it > 0 }

        // Validate: For music, must select from Spotify
        if (selectedType == "music" && selectedSearchResult == null) {
            Toast.makeText(this, "Vui lòng tìm kiếm và chọn bài hát từ Spotify", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate title
        if (title.isEmpty()) {
            binding.tilTitle.error = "Vui lòng nhập tên"
            return
        }
        binding.tilTitle.error = null

        if (selectedCollectionId == null) {
            Toast.makeText(this, "Vui lòng chọn collection", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@AddItemActivity, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }
                
                // Determine cover image URL
                var coverImageUrl: String? = null
                
                if (selectedSearchResult != null) {
                    // For music/movie from API - use the API cover URL directly
                    coverImageUrl = selectedCoverUrl
                } else if (selectedImageUri != null && selectedType != "music" && selectedType != "movie") {
                    // For other types with manual image selection - upload to server
                    Toast.makeText(this@AddItemActivity, "Đang upload ảnh...", Toast.LENGTH_SHORT).show()
                    coverImageUrl = uploadImage(token, selectedImageUri!!)
                    if (coverImageUrl == null) {
                        Log.w(TAG, "Image upload failed, continuing without cover")
                    }
                }

                // Build metadata based on type
                val metadata = buildMetadata()

                // Step 1: Create the item
                val itemBody = mutableMapOf<String, Any?>(
                    "type" to selectedType,
                    "title" to title,
                    "description" to description,
                    "metadata" to metadata
                )
                
                // Add external_id as separate field (for Spotify/TMDB)
                selectedSearchResult?.let { result ->
                    itemBody["external_id"] = result.externalId
                }
                
                // Add cover_image_url as separate field
                coverImageUrl?.let { url ->
                    itemBody["cover_image_url"] = url
                }

                val itemResponse = RetrofitClient.apiService.createItem(
                    token = "Bearer $token",
                    body = itemBody
                )

                if (!itemResponse.isSuccessful) {
                    val errorBody = itemResponse.errorBody()?.string()
                    val errorMessage = try {
                        val json = JSONObject(errorBody ?: "")
                        json.optString("error").ifEmpty { json.optString("message", "Lỗi tạo item") }
                    } catch (e: Exception) {
                        "Lỗi tạo item"
                    }
                    if (errorMessage.contains("external_id", true) || errorMessage.contains("tồn tại", true)) {
                        Toast.makeText(
                            this@AddItemActivity,
                            "Item đã tồn tại. Hãy chọn collection khác hoặc bỏ qua bản trùng.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(this@AddItemActivity, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                    showLoading(false)
                    return@launch
                }

                val itemData = itemResponse.body()?.get("data") as? Map<*, *>
                val itemId = (itemData?.get("id") as? Number)?.toInt()

                if (itemId == null) {
                    Toast.makeText(this@AddItemActivity, "Lỗi: Không lấy được ID item", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }

                // Step 2: Add item to collection
                val collectionItemBody = mutableMapOf<String, Any?>(
                    "item_id" to itemId
                )
                if (rating != null) {
                    collectionItemBody["rating"] = rating
                }

                val addToCollectionResponse = RetrofitClient.apiService.addItemToCollection(
                    token = "Bearer $token",
                    collectionId = selectedCollectionId!!,
                    body = collectionItemBody
                )

                if (addToCollectionResponse.isSuccessful) {
                    Toast.makeText(this@AddItemActivity, "Thêm item thành công!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = addToCollectionResponse.errorBody()?.string()
                    val errorMessage = try {
                        JSONObject(errorBody ?: "").optString("message", "Lỗi thêm vào collection")
                    } catch (e: Exception) {
                        "Lỗi thêm vào collection"
                    }
                    Toast.makeText(this@AddItemActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error adding item", e)
                Toast.makeText(this@AddItemActivity, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun buildMetadata(): Map<String, Any?> {
        val metadata = mutableMapOf<String, Any?>()

        when (selectedType) {
            "music" -> {
                val artist = binding.etArtist.text.toString().trim()
                val album = binding.etAlbum.text.toString().trim()
                if (artist.isNotEmpty()) metadata["artist"] = artist
                if (album.isNotEmpty()) metadata["album"] = album
            }
            "movie" -> {
                val director = binding.etDirector.text.toString().trim()
                if (director.isNotEmpty()) metadata["director"] = director
            }
            "book" -> {
                val author = binding.etAuthor.text.toString().trim()
                if (author.isNotEmpty()) metadata["author"] = author
            }
            "game" -> {
                val developer = binding.etDirector.text.toString().trim()
                if (developer.isNotEmpty()) metadata["developer"] = developer
            }
        }
        
        // Add source info from search result (spotify or tmdb)
        selectedSearchResult?.let { result ->
            metadata["source"] = if (selectedType == "music") "spotify" else "tmdb"
            // Also store preview_url for music if available
            result.metadata?.get("preview_url")?.let { metadata["preview_url"] = it }
            result.metadata?.get("spotify_url")?.let { metadata["spotify_url"] = it }
            // Store TMDB info
            result.metadata?.get("vote_average")?.let { metadata["vote_average"] = it }
            result.metadata?.get("overview")?.let { metadata["overview"] = it }
        }

        return metadata
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnAdd.isEnabled = !show
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
            val folderPart = "items".toRequestBody("text/plain".toMediaTypeOrNull())
            
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
                Log.e(TAG, "Upload failed: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            null
        }
    }
}
