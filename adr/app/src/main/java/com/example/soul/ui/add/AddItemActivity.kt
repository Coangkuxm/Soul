package com.example.soul.ui.add

import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.Collection
import com.example.soul.data.model.SearchResult
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityAddItemBinding
import com.example.soul.ui.add.adapter.SearchResultAdapter
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
    private var selectedSearchResult: SearchResult? = null
    private var selectedCoverUrl: String? = null
    private var selectedImageUri: Uri? = null

    private lateinit var trendingAdapter: SearchResultAdapter
    private var trendingLoadedType: String? = null

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedCoverUrl = null
            selectedSearchResult = null
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
        setupTrending()
        setupTypeChips()
        setupListeners()
        loadCollections()
    }

    private fun setupTrending() {
        trendingAdapter = SearchResultAdapter { result -> fillFormFromSearchResult(result) }
        binding.rvTrending.layoutManager = LinearLayoutManager(this)
        binding.rvTrending.adapter = trendingAdapter
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
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

                if (newType != selectedType) {
                    selectedSearchResult = null
                    clearForm()
                }

                selectedType = newType
                updateMetadataFields()
            }
        }
        updateMetadataFields()
    }

    private fun clearForm() {
        binding.etTitle.text?.clear()
        binding.etPostNote.text?.clear()
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
        binding.tilArtist.visibility = View.GONE
        binding.tilAlbum.visibility = View.GONE
        binding.tilDirector.visibility = View.GONE
        binding.tilAuthor.visibility = View.GONE

        val needsSearch = selectedType == "music" || selectedType == "movie"
        binding.btnSearch.visibility = if (needsSearch) View.VISIBLE else View.GONE

        val hasSelection = selectedSearchResult != null
        binding.layoutContent.visibility = if (needsSearch && !hasSelection) View.GONE else View.VISIBLE

        // Loại không phải nhạc: cho phép nhập tên thủ công trở lại.
        if (selectedType != "music") {
            setFieldLocked(
                binding.etTitle,
                false,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            )
        }

        when (selectedType) {
            "music" -> binding.btnSearch.text = getString(R.string.add_item_search_spotify)
            "movie" -> binding.btnSearch.text = getString(R.string.add_item_search_tmdb)
        }

        when (selectedType) {
            "music" -> {
                binding.tilArtist.visibility = View.VISIBLE
                binding.tilArtist.hint = getString(R.string.add_item_artist_hint)
                binding.tilAlbum.visibility = View.VISIBLE
                setMusicFieldsEnabled(hasSelection)
            }
            "movie" -> {
                binding.tilDirector.visibility = View.VISIBLE
                binding.tilDirector.hint = getString(R.string.add_item_director_hint)
            }
            "book" -> {
                binding.tilAuthor.visibility = View.VISIBLE
            }
            "artist" -> Unit
            "game" -> {
                binding.tilDirector.visibility = View.VISIBLE
                binding.tilDirector.hint = getString(R.string.add_item_developer_hint)
            }
        }

        maybeLoadTrending()
    }

    /** Hiện danh sách "đang thịnh hành" ngay trên màn (nhạc/phim) khi chưa chọn item nào. */
    private fun maybeLoadTrending() {
        val needsSearch = selectedType == "music" || selectedType == "movie"
        val show = needsSearch && selectedSearchResult == null
        binding.tvTrendingLabel.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvTrending.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) return

        binding.tvTrendingLabel.text =
            if (selectedType == "music") "🔥 Nhạc đang thịnh hành" else "🔥 Phim đang thịnh hành"

        if (trendingLoadedType == selectedType) return
        trendingLoadedType = selectedType
        val typeForLoad = selectedType
        trendingAdapter.submitList(emptyList())

        lifecycleScope.launch {
            try {
                val results = if (typeForLoad == "music") loadTrendingMusic() else loadTrendingMovies()
                // Bỏ qua nếu người dùng đã đổi loại hoặc đã chọn item trong lúc tải.
                if (selectedType == typeForLoad && selectedSearchResult == null) {
                    trendingAdapter.submitList(results)
                }
            } catch (e: Exception) {
                trendingLoadedType = null // cho phép thử lại lần sau
                Log.e(TAG, "Trending load error", e)
            }
        }
    }

    private suspend fun loadTrendingMusic(): List<SearchResult> {
        val response = RetrofitClient.apiService.getTrendingSpotify()
        if (response.isSuccessful && response.body()?.success == true) {
            return response.body()!!.data.map { track ->
                SearchResult(
                    id = track.id,
                    title = track.name,
                    subtitle = track.artists,
                    extra = track.album,
                    coverUrl = track.coverUrl,
                    type = "music",
                    externalId = track.id,
                    metadata = mapOf(
                        "artist" to track.artists,
                        "album" to track.album,
                        "preview_url" to track.previewUrl,
                        "spotify_url" to track.externalUrl
                    )
                )
            }
        }
        return emptyList()
    }

    private suspend fun loadTrendingMovies(): List<SearchResult> {
        val response = RetrofitClient.apiService.getTrendingTMDB()
        if (response.isSuccessful && response.body()?.success == true) {
            return response.body()!!.data.results
                .filter { it.title != null || it.name != null }
                .map { result ->
                    val type = if (result.mediaType == "tv") "tv" else "movie"
                    SearchResult(
                        id = result.id.toString(),
                        title = result.getDisplayTitle(),
                        subtitle = if (type == "tv") "📺 TV Series" else "🎬 Movie",
                        extra = result.getYear()?.let { "($it)" } ?: "",
                        coverUrl = result.getPosterUrl(),
                        type = type,
                        externalId = result.id.toString(),
                        metadata = mapOf(
                            "tmdb_id" to result.id,
                            "overview" to result.overview,
                            "release_date" to (result.releaseDate ?: result.firstAirDate),
                            "vote_average" to result.voteAverage,
                            "media_type" to type
                        )
                    )
                }
        }
        return emptyList()
    }

    private fun setMusicFieldsEnabled(hasSelection: Boolean) {
        // Tên / nghệ sĩ / album là dữ liệu từ Spotify -> khóa, chỉ đọc.
        setFieldLocked(binding.etTitle, true, 0)
        setFieldLocked(binding.etArtist, true, 0)
        setFieldLocked(binding.etAlbum, true, 0)
        binding.etPostNote.isEnabled = true

        binding.tilTitle.hint = if (!hasSelection && selectedType == "music") {
            getString(R.string.add_item_music_name_hint)
        } else {
            getString(R.string.add_item_name_hint)
        }
    }

    /** Khóa/mở khóa chỉnh sửa một ô nhập mà vẫn hiển thị chữ rõ ràng. */
    private fun setFieldLocked(
        et: com.google.android.material.textfield.TextInputEditText,
        locked: Boolean,
        normalInputType: Int
    ) {
        if (locked) {
            et.inputType = InputType.TYPE_NULL
            et.isFocusable = false
            et.isFocusableInTouchMode = false
            et.isCursorVisible = false
        } else {
            et.isFocusableInTouchMode = true
            et.isFocusable = true
            et.isCursorVisible = true
            et.inputType = normalInputType
        }
    }

    private fun setupListeners() {
        binding.cardCover.setOnClickListener {
            if (selectedSearchResult != null) {
                Toast.makeText(
                    this,
                    getString(
                        R.string.add_item_cover_from_source,
                        if (selectedType == "music") "Spotify" else "TMDB"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (selectedType != "music" && selectedType != "movie") {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                Toast.makeText(
                    this,
                    getString(
                        R.string.add_item_search_cover_prompt,
                        if (selectedType == "music") "Spotify" else "TMDB"
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnSearch.setOnClickListener { showSearchBottomSheet() }

        binding.actvCollection.setOnItemClickListener { _, _, position, _ ->
            if (position < collections.size) {
                selectedCollectionId = collections[position].id
            }
        }

        binding.btnAdd.setOnClickListener { addItem() }
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
        selectedImageUri = null
        binding.tvTrendingLabel.visibility = View.GONE
        binding.rvTrending.visibility = View.GONE
        binding.layoutContent.visibility = View.VISIBLE
        binding.etTitle.setText(result.title)
        binding.etPostNote.text?.clear()

        result.coverUrl?.let { url ->
            Glide.with(this)
                .load(url)
                .placeholder(R.drawable.bg_placeholder_cover)
                .error(R.drawable.bg_placeholder_cover)
                .centerCrop()
                .into(binding.ivCover)
        }

        when (selectedType) {
            "music" -> {
                result.subtitle?.let { binding.etArtist.setText(it) }
                result.metadata?.get("album")?.let { binding.etAlbum.setText(it.toString()) }
                setMusicFieldsEnabled(true)
            }
            "movie" -> Unit
        }

        Toast.makeText(this, getString(R.string.add_item_selected, result.title), Toast.LENGTH_SHORT).show()
    }

    private fun loadCollections() {
        lifecycleScope.launch {
            try {
                val token = authPreferences.getToken() ?: return@launch
                val response = RetrofitClient.apiService.getCollections(
                    token = "Bearer $token",
                    userId = authPreferences.getUser()?.id,
                    limit = 50
                )

                if (response.isSuccessful && response.body() != null) {
                    collections = response.body()!!.data
                    val adapter = ArrayAdapter(
                        this@AddItemActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        collections.map { it.name }
                    )
                    binding.actvCollection.setAdapter(adapter)

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
        val note = binding.etPostNote.text.toString().trim()

        if (selectedType == "music" && selectedSearchResult == null) {
            Toast.makeText(this, R.string.add_item_select_music_required, Toast.LENGTH_SHORT).show()
            return
        }

        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.add_item_name_required)
            return
        }
        binding.tilTitle.error = null

        if (selectedCollectionId == null) {
            Toast.makeText(this, R.string.add_item_collection_required, Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    Toast.makeText(this@AddItemActivity, R.string.add_item_login_again, Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }

                var coverImageUrl: String? = null
                if (selectedSearchResult != null) {
                    coverImageUrl = selectedCoverUrl
                } else if (selectedImageUri != null && selectedType != "music" && selectedType != "movie") {
                    Toast.makeText(this@AddItemActivity, R.string.add_item_uploading_image, Toast.LENGTH_SHORT).show()
                    coverImageUrl = uploadImage(token, selectedImageUri!!)
                    if (coverImageUrl == null) {
                        Log.w(TAG, "Image upload failed, continuing without cover")
                    }
                }

                val metadata = buildMetadata()
                val itemBody = mutableMapOf<String, Any?>(
                    "type" to selectedType,
                    "title" to title,
                    "description" to "",
                    "metadata" to metadata
                )

                selectedSearchResult?.let { result ->
                    itemBody["external_id"] = result.externalId
                }
                coverImageUrl?.let { url ->
                    itemBody["cover_image_url"] = url
                }

                val itemId = resolveItemId(
                    token = token,
                    itemBody = itemBody,
                    externalId = selectedSearchResult?.externalId
                )

                if (itemId == null) {
                    Toast.makeText(this@AddItemActivity, R.string.add_item_missing_id, Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }

                val collectionItemBody = mutableMapOf<String, Any?>("item_id" to itemId)
                if (note.isNotEmpty()) {
                    collectionItemBody["note"] = note
                }

                val addToCollectionResponse = RetrofitClient.apiService.addItemToCollection(
                    token = "Bearer $token",
                    collectionId = selectedCollectionId!!,
                    body = collectionItemBody
                )

                if (addToCollectionResponse.isSuccessful) {
                    Toast.makeText(this@AddItemActivity, R.string.add_item_success, Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = addToCollectionResponse.errorBody()?.string()
                    val errorMessage = try {
                        JSONObject(errorBody ?: "").optString("message", getString(R.string.add_item_collection_error))
                    } catch (_: Exception) {
                        getString(R.string.add_item_collection_error)
                    }
                    Toast.makeText(this@AddItemActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding item", e)
                Toast.makeText(
                    this@AddItemActivity,
                    getString(R.string.generic_error_with_message, e.message ?: ""),
                    Toast.LENGTH_SHORT
                ).show()
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

        selectedSearchResult?.let { result ->
            metadata["source"] = if (selectedType == "music") "spotify" else "tmdb"
            result.metadata?.get("preview_url")?.let { metadata["preview_url"] = it }
            result.metadata?.get("spotify_url")?.let { metadata["spotify_url"] = it }
            result.metadata?.get("vote_average")?.let { metadata["vote_average"] = it }
            result.metadata?.get("overview")?.let { metadata["overview"] = it }
        }

        return metadata
    }

    private suspend fun resolveItemId(
        token: String,
        itemBody: Map<String, Any?>,
        externalId: String?
    ): Int? {
        if (!externalId.isNullOrBlank()) {
            try {
                val existingResponse = RetrofitClient.apiService.getItemByExternalId(
                    token = "Bearer $token",
                    externalId = externalId
                )
                if (existingResponse.isSuccessful) {
                    val existingData = existingResponse.body()?.get("data") as? Map<*, *>
                    val existingId = (existingData?.get("id") as? Number)?.toInt()
                    if (existingId != null) return existingId
                }
            } catch (_: Exception) {
            }
        }

        val itemResponse = RetrofitClient.apiService.createItem(
            token = "Bearer $token",
            body = itemBody
        )

        if (!itemResponse.isSuccessful) {
            val errorBody = itemResponse.errorBody()?.string()
            val errorMessage = try {
                val json = JSONObject(errorBody ?: "")
                json.optString("error").ifEmpty { json.optString("message", getString(R.string.add_item_create_error)) }
            } catch (_: Exception) {
                getString(R.string.add_item_create_error)
            }

            if (!externalId.isNullOrBlank() && errorMessage.contains("external_id", true)) {
                try {
                    val existingResponse = RetrofitClient.apiService.getItemByExternalId(
                        token = "Bearer $token",
                        externalId = externalId
                    )
                    if (existingResponse.isSuccessful) {
                        val existingData = existingResponse.body()?.get("data") as? Map<*, *>
                        val existingId = (existingData?.get("id") as? Number)?.toInt()
                        if (existingId != null) return existingId
                    }
                } catch (_: Exception) {
                }
            }

            Toast.makeText(this@AddItemActivity, errorMessage, Toast.LENGTH_SHORT).show()
            return null
        }

        val itemData = itemResponse.body()?.get("data") as? Map<*, *>
        return (itemData?.get("id") as? Number)?.toInt()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnAdd.isEnabled = !show
    }

    private suspend fun uploadImage(token: String, uri: Uri): String? {
        return try {
            if (!ImagePickerHelper.isFileSizeValid(this, uri)) {
                Toast.makeText(this, R.string.add_item_image_too_large, Toast.LENGTH_SHORT).show()
                return null
            }

            val inputStream = contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes == null) return null

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
