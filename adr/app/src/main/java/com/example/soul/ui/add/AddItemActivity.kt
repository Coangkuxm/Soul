package com.example.soul.ui.add

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.Collection
import com.example.soul.data.model.SearchResult
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityAddItemBinding
import kotlinx.coroutines.launch
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
        binding.etYear.text?.clear()
        binding.ivCover.setImageResource(R.drawable.bg_placeholder_cover)
        selectedCoverUrl = null
    }

    private fun updateMetadataFields() {
        // Hide all optional fields first
        binding.tilArtist.visibility = View.GONE
        binding.tilAlbum.visibility = View.GONE
        binding.tilDirector.visibility = View.GONE
        binding.tilAuthor.visibility = View.GONE
        
        // Show/hide search button based on type
        binding.btnSearch.visibility = if (selectedType == "music" || selectedType == "movie") {
            View.VISIBLE
        } else {
            View.GONE
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

    private fun setupListeners() {
        // Cover image picker / Search button
        binding.cardCover.setOnClickListener {
            if (selectedType == "music" || selectedType == "movie") {
                showSearchBottomSheet()
            } else {
                Toast.makeText(this, "Chọn ảnh bìa - sẽ cập nhật sau", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Search button
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
        
        // Fill title
        binding.etTitle.setText(result.title)
        
        // Fill description if available
        result.metadata?.get("subtitle")?.let {
            binding.etDescription.setText(it.toString())
        }
        
        // Load cover image
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
            }
            "movie" -> {
                // Fill year from metadata
                result.metadata?.get("release_date")?.let { dateStr ->
                    val year = dateStr.toString().split("-").firstOrNull()
                    year?.let { binding.etYear.setText(it) }
                }
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
        val note = binding.etNote.text.toString().trim()
        val rating = binding.ratingBar.rating.toInt().takeIf { it > 0 }

        // Validate
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

                // Build metadata based on type
                val metadata = buildMetadata()

                // Step 1: Create the item
                val itemBody = mutableMapOf<String, Any?>(
                    "type" to selectedType,
                    "title" to title,
                    "description" to description,
                    "metadata" to metadata
                )

                val itemResponse = RetrofitClient.apiService.createItem(
                    token = "Bearer $token",
                    body = itemBody
                )

                if (!itemResponse.isSuccessful) {
                    val errorBody = itemResponse.errorBody()?.string()
                    val errorMessage = try {
                        JSONObject(errorBody ?: "").optString("message", "Lỗi tạo item")
                    } catch (e: Exception) {
                        "Lỗi tạo item"
                    }
                    Toast.makeText(this@AddItemActivity, errorMessage, Toast.LENGTH_SHORT).show()
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
                if (note.isNotEmpty()) {
                    collectionItemBody["note"] = note
                }
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
        val year = binding.etYear.text.toString().trim()

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

        if (year.isNotEmpty()) {
            metadata["year"] = year.toIntOrNull()
        }
        
        // Add cover URL from search result or manual input
        selectedCoverUrl?.let { metadata["cover_url"] = it }
        
        // Add external_id from search result (Spotify ID or TMDB ID)
        selectedSearchResult?.let { result ->
            metadata["external_id"] = result.externalId
            metadata["source"] = if (selectedType == "music") "spotify" else "tmdb"
        }

        return metadata
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnAdd.isEnabled = !show
    }
}
