package com.example.soul.ui.add

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.soul.data.model.SearchResult
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.DialogSearchMediaBinding
import com.example.soul.ui.add.adapter.SearchResultAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchMediaBottomSheet private constructor() : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "SearchMediaBottomSheet"
        private const val DEBOUNCE_DELAY = 500L
        private const val ARG_MEDIA_TYPE = "media_type"
        
        private var callback: ((SearchResult) -> Unit)? = null
        
        fun newInstance(
            mediaType: String,
            onItemSelected: (SearchResult) -> Unit
        ): SearchMediaBottomSheet {
            callback = onItemSelected
            return SearchMediaBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_MEDIA_TYPE, mediaType)
                }
            }
        }
    }

    private var _binding: DialogSearchMediaBinding? = null
    private val binding get() = _binding!!
    
    private val mediaType: String by lazy {
        arguments?.getString(ARG_MEDIA_TYPE) ?: "music"
    }
    
    private val onItemSelected: (SearchResult) -> Unit = { result ->
        callback?.invoke(result)
    }

    private lateinit var adapter: SearchResultAdapter
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSearchMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupRecyclerView()
        setupSearch()
    }

    private fun setupUI() {
        binding.tvTitle.text = when (mediaType) {
            "music" -> "🎵 Tìm kiếm nhạc từ Spotify"
            "movie" -> "🎬 Tìm kiếm phim từ TMDB"
            else -> "Tìm kiếm"
        }

        binding.tilSearch.hint = when (mediaType) {
            "music" -> "Tên bài hát, nghệ sĩ..."
            "movie" -> "Tên phim, TV show..."
            else -> "Từ khóa tìm kiếm"
        }

        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = "Nhập từ khóa để tìm kiếm"
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter { result ->
            onItemSelected(result)
            dismiss()
        }

        binding.rvResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SearchMediaBottomSheet.adapter
        }
    }

    private fun setupSearch() {
        // Search on text change with debounce
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 2) {
                    searchJob = lifecycleScope.launch {
                        delay(DEBOUNCE_DELAY)
                        performSearch(query)
                    }
                } else {
                    adapter.submitList(emptyList())
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "Nhập ít nhất 2 ký tự"
                }
            }
        })

        // Search on keyboard action
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text?.toString()?.trim() ?: ""
                if (query.length >= 2) {
                    searchJob?.cancel()
                    lifecycleScope.launch { performSearch(query) }
                }
                true
            } else false
        }
    }

    private suspend fun performSearch(query: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        try {
            val results = when (mediaType) {
                "music" -> searchSpotify(query)
                "movie" -> searchTMDB(query)
                else -> emptyList()
            }

            if (results.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Không tìm thấy kết quả"
            }

            adapter.submitList(results)
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = "Lỗi: ${e.message}"
        } finally {
            binding.progressBar.visibility = View.GONE
        }
    }

    private suspend fun searchSpotify(query: String): List<SearchResult> {
        val response = RetrofitClient.apiService.searchSpotify(query)
        
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

    private suspend fun searchTMDB(query: String): List<SearchResult> {
        val response = RetrofitClient.apiService.searchTMDB(query)
        
        if (response.isSuccessful && response.body()?.success == true) {
            return response.body()!!.data.results
                .filter { it.title != null || it.name != null }
                .map { result ->
                    val type = if (result.mediaType == "tv") "tv" else "movie"
                    SearchResult(
                        id = result.id.toString(),
                        title = result.getDisplayTitle(),
                        subtitle = when {
                            type == "tv" -> "📺 TV Series"
                            else -> "🎬 Movie"
                        },
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

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
