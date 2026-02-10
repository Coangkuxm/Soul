package com.example.soul.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.soul.R
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.FeedItem
import com.example.soul.data.remote.DeezerRetrofitClient
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityFeedBinding
import com.example.soul.ui.add.AddCollectionActivity
import com.example.soul.ui.add.AddItemActivity
import com.example.soul.ui.add.AddOptionsBottomSheet
import com.example.soul.ui.auth.LoginActivity
import com.example.soul.ui.home.adapter.FeedAdapter
import com.example.soul.audio.PreviewAudioPlayer
import com.example.soul.utils.Resource
import kotlinx.coroutines.launch

class FeedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedBinding
    private lateinit var authPreferences: AuthPreferences
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var previewAudioPlayer: PreviewAudioPlayer
    private var currentPreviewUrl: String? = null
    private var currentPreviewTitle: String? = null
    private var currentPreviewArtist: String? = null
    private var currentPreviewCoverUrl: String? = null
    private var scrollToTopOnNextRefresh: Boolean = false

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(this)
    }

    // Activity result launchers
    private val addCollectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.refresh()
        }
    }

    private val addItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authPreferences = AuthPreferences(this)
        previewAudioPlayer = PreviewAudioPlayer(this)

        // Check if user is logged in
        if (authPreferences.getToken().isNullOrEmpty()) {
            navigateToLogin()
            return
        }

        setupUI()
        setupRecyclerView()
        setupObservers()
        setupListeners()
        if (intent.getBooleanExtra("refresh", false)) {
            scrollToTopOnNextRefresh = true
            viewModel.refresh()
        }    }

    private fun setupUI() {
        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.primary_dark
        )
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(
            onItemClick = { feedItem -> 
                onFeedItemClicked(feedItem) 
            },
            onUserClick = { userId ->
                onUserClicked(userId)
            },
            onLikeClick = { feedItem, isLiked ->
                handleLikeClick(feedItem, isLiked)
            },
            onPlayClick = { feedItem ->
                handlePlayClick(feedItem)
            }
        )

        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(this@FeedActivity)
            adapter = feedAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        // Observe profile for header avatar
        viewModel.profile.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show default avatar while loading
                    binding.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
                }
                is Resource.Success -> {
                    resource.data?.let { profile ->
                        // Load user avatar in header
                        val avatarUrl = profile.avatarUrl
                        val isValidUrl = !avatarUrl.isNullOrEmpty() && 
                            !avatarUrl.contains("example.com") &&
                            (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))
                        
                        if (isValidUrl) {
                            Glide.with(this@FeedActivity)
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .circleCrop()
                                .into(binding.ivUserAvatar)
                        } else {
                            binding.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar)
                }
            }
        }

        // Observe feed items
        viewModel.feedItems.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.layoutEmpty.visibility = View.GONE
                }
                is Resource.Success -> {
                    resource.data?.let { items ->
                        if (items.isEmpty()) {
                            binding.layoutEmpty.visibility = View.VISIBLE
                            binding.rvFeed.visibility = View.GONE
                        } else {
                            binding.layoutEmpty.visibility = View.GONE
                            binding.rvFeed.visibility = View.VISIBLE
                            feedAdapter.submitList(items) {
                            if (scrollToTopOnNextRefresh) {
                                binding.rvFeed.scrollToPosition(0)
                                scrollToTopOnNextRefresh = false
                            }
                        }
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvFeed.visibility = View.GONE
                }
            }
        }

        // Observe refresh state
        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }

        // Observe session expired
        viewModel.sessionExpired.observe(this) { expired ->
            if (expired) {
                Toast.makeText(this, "Phiên đăng nhập đã hết hạn", Toast.LENGTH_SHORT).show()
                logout()
            }
        }
    }

    private fun setupListeners() {
        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        // User avatar click - go to own profile
        binding.ivUserAvatar.setOnClickListener {
            Toast.makeText(this, "Your profile coming soon", Toast.LENGTH_SHORT).show()
        }

        // Filter dropdown
        binding.btnFilter.setOnClickListener {
            showFilterMenu(it)
        }

        // Notification button
        // Header add button
        binding.btnAdd.setOnClickListener {
            showAddOptionsBottomSheet()
        }
        // Bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home/feed
                    true
                }
                R.id.nav_explore -> {
                    Toast.makeText(this, "Explore coming soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_notification -> {
                    true
                }
                R.id.nav_library -> {
                    // Navigate to library/collections
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    // Logout
                    logout()
                    true
                }
                else -> false
            }
        }
        binding.ivMiniPlayerPlay.setOnClickListener {
            val url = currentPreviewUrl
            if (!url.isNullOrEmpty()) {
                previewAudioPlayer.toggle(url)
            }
        }

    }

    private fun showFilterMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Everyone")
            menu.add("Friends Only")
            setOnMenuItemClickListener { item ->
                binding.btnFilter.text = item.title
                // TODO: Filter feed based on selection
                viewModel.refresh()
                true
            }
            show()
        }
    }

    private fun showAddOptionsBottomSheet() {
        val bottomSheet = AddOptionsBottomSheet(
            onAddCollection = {
                addCollectionLauncher.launch(Intent(this, AddCollectionActivity::class.java))
            },
            onAddItem = {
                addItemLauncher.launch(Intent(this, AddItemActivity::class.java))
            }
        )
        bottomSheet.show(supportFragmentManager, AddOptionsBottomSheet.TAG)
    }

    private fun onFeedItemClicked(feedItem: FeedItem) {
        Toast.makeText(this, "Opening ${feedItem.item.title}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to item detail or play
    }

    private fun onUserClicked(userId: Int) {
        Toast.makeText(this, "Opening user profile", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to user profile
    }

    private var pendingSpotifyUrlForPreview: String? = null
    private val miniPlayerUpdater = object : Runnable {
        override fun run() {
            val pos = previewAudioPlayer.getPositionMs()
            val dur = previewAudioPlayer.getDurationMs()
            if (dur > 0) {
                binding.tvMiniPlayerTime.text = "${formatTime(pos)}/${formatTime(dur)}"
            } else {
                binding.tvMiniPlayerTime.text = "0:00/0:30"
            }
            binding.ivMiniPlayerPlay.setImageResource(
                if (previewAudioPlayer.isPlaying()) R.drawable.ic_pause_circle else R.drawable.ic_play_circle
            )
            binding.layoutMiniPlayer.postDelayed(this, 500)
        }
    }

    private fun handlePlayClick(feedItem: FeedItem) {
        val previewUrl = feedItem.item.metadata?.previewUrl
        val spotifyUrl = feedItem.item.metadata?.spotifyUrl
        currentPreviewTitle = feedItem.item.title
        currentPreviewArtist = feedItem.item.metadata?.artist
        currentPreviewCoverUrl = feedItem.item.coverImageUrl

        if (!previewUrl.isNullOrEmpty()) {
            pendingSpotifyUrlForPreview = spotifyUrl
            previewAudioPlayer.setOnEndedListener {
                stopMiniPlayer()
                promptOpenSpotifyIfAvailable()
            }
            startMiniPlayer()
            currentPreviewUrl = previewUrl
            previewAudioPlayer.toggle(previewUrl)
            return
        }

        // Try Deezer preview by track name + artist
        val title = feedItem.item.title
        val artist = feedItem.item.metadata?.artist
        val query = if (!artist.isNullOrEmpty()) "$title $artist" else title

        lifecycleScope.launch {
            try {
                val response = DeezerRetrofitClient.apiService.search(query)
                val deezerPreview = response.data.firstOrNull { !it.preview.isNullOrEmpty() }?.preview

                if (!deezerPreview.isNullOrEmpty()) {
                    pendingSpotifyUrlForPreview = spotifyUrl
                    previewAudioPlayer.setOnEndedListener {
                        stopMiniPlayer()
                        promptOpenSpotifyIfAvailable()
                    }
                    startMiniPlayer()
                    currentPreviewUrl = deezerPreview
                    previewAudioPlayer.toggle(deezerPreview)
                } else {
                    promptOpenSpotifyOrNotify(spotifyUrl)
                }
            } catch (e: Exception) {
                promptOpenSpotifyOrNotify(spotifyUrl)
            }
        }
    }

    private fun promptOpenSpotifyIfAvailable() {
        val url = pendingSpotifyUrlForPreview
        if (url.isNullOrEmpty()) return
        pendingSpotifyUrlForPreview = null
        promptOpenSpotifyOrNotify(url)
    }

    private fun promptOpenSpotifyOrNotify(spotifyUrl: String?) {
        if (!spotifyUrl.isNullOrEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Hết preview")
                .setMessage("Bạn muốn mở Spotify để nghe cả bài không?")
                .setPositiveButton("Mở Spotify") { _, _ ->
                    openSpotify(spotifyUrl)
                }
                .setNegativeButton("Hủy", null)
                .show()
        } else {
            Toast.makeText(this, "Không có nguồn phát", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSpotify(spotifyUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(spotifyUrl))
            intent.setPackage("com.spotify.music")
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(spotifyUrl))
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Không thể mở Spotify", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun startMiniPlayer() {
        val title = currentPreviewTitle ?: "Preview"
        val artist = currentPreviewArtist
        binding.tvMiniPlayerTitle.text = if (!artist.isNullOrEmpty()) "$title � $artist" else title
        val coverUrl = currentPreviewCoverUrl
        if (!coverUrl.isNullOrEmpty() && !coverUrl.contains("example.com")) {
            Glide.with(this)
                .load(coverUrl)
                .placeholder(R.drawable.ic_default_cover)
                .error(R.drawable.ic_default_cover)
                .centerCrop()
                .into(binding.ivMiniPlayerCover)
        } else {
            binding.ivMiniPlayerCover.setImageResource(R.drawable.ic_default_cover)
        }
        binding.layoutMiniPlayer.visibility = View.VISIBLE
        binding.layoutMiniPlayer.removeCallbacks(miniPlayerUpdater)
        binding.layoutMiniPlayer.post(miniPlayerUpdater)
    }

    private fun stopMiniPlayer() {
        binding.layoutMiniPlayer.removeCallbacks(miniPlayerUpdater)
        binding.layoutMiniPlayer.visibility = View.GONE
        binding.tvMiniPlayerTime.text = "0:00/0:30"
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    private fun handleLikeClick(feedItem: FeedItem, isLiked: Boolean) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${authPreferences.getToken()}"
                val body = mapOf(
                    "targetId" to feedItem.item.id,
                    "targetType" to "item"
                )
                
                if (isLiked) {
                    RetrofitClient.apiService.likeItem(token, body)
                } else {
                    RetrofitClient.apiService.unlikeItem(token, body)
                }
            } catch (e: Exception) {
                // Revert on error
                feedItem.isLiked = !isLiked
                feedAdapter.notifyDataSetChanged()
                Toast.makeText(this@FeedActivity, "Failed to update like", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        authPreferences.clearSession()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.layoutMiniPlayer.removeCallbacks(miniPlayerUpdater)
        previewAudioPlayer.release()
    }
}




































