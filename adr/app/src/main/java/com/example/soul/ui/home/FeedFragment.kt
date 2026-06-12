package com.example.soul.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.soul.R
import com.example.soul.audio.PreviewAudioPlayer
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.FeedItem
import com.example.soul.data.remote.DeezerRetrofitClient
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.ActivityFeedBinding
import com.example.soul.ui.add.AddCollectionActivity
import com.example.soul.ui.add.AddItemActivity
import com.example.soul.ui.add.AddOptionsBottomSheet
import com.example.soul.ui.comments.CommentsActivity
import com.example.soul.ui.home.adapter.FeedAdapter
import com.example.soul.ui.main.MainTabsActivity
import com.example.soul.ui.messenger.SharePostActivity
import com.example.soul.ui.profile.UserProfileActivity
import com.example.soul.utils.Resource
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {

    private var _binding: ActivityFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var authPreferences: AuthPreferences
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var previewAudioPlayer: PreviewAudioPlayer
    private var currentPreviewUrl: String? = null
    private var currentPreviewTitle: String? = null
    private var currentPreviewArtist: String? = null
    private var currentPreviewCoverUrl: String? = null
    private var scrollToTopOnNextRefresh: Boolean = true

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(requireContext())
    }

    private val addCollectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) viewModel.refresh()
    }

    private val addItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) viewModel.refresh()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authPreferences = AuthPreferences(requireContext())
        previewAudioPlayer = PreviewAudioPlayer(requireContext())

        // Bottom nav is provided by MainTabsActivity
        binding.bottomNavigation.visibility = View.GONE

        // Show cached avatar immediately (avoids default flash)
        setAvatarFromUrl(authPreferences.getUser()?.avatarUrl)

        setupUI()
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupUI() {
        binding.swipeRefresh.setColorSchemeResources(R.color.primary, R.color.primary_dark)
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(
            onItemClick = { openComments(it) },
            onUserClick = { onUserClicked(it) },
            onLikeClick = { feedItem, isLiked -> handleLikeClick(feedItem, isLiked) },
            onPlayClick = { handlePlayClick(it) },
            onCommentClick = { openComments(it) },
            onShareClick = { openSharePost(it) },
            onReportClick = { feedItem, anchor -> showFeedItemMenu(feedItem, anchor) }
        )
        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewModel.profile.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    setAvatarFromUrl(resource.data?.avatarUrl)
                }
                else -> Unit
            }
        }

        viewModel.feedItems.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    val items = resource.data.orEmpty()
                    val sorted = items.sortedByDescending { it.addedAt ?: "" }
                    if (items.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.rvFeed.visibility = View.GONE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvFeed.visibility = View.VISIBLE
                        feedAdapter.submitList(sorted) {
                            if (scrollToTopOnNextRefresh) {
                                binding.rvFeed.scrollToPosition(0)
                                scrollToTopOnNextRefresh = false
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvFeed.visibility = View.GONE
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show()
                }
                else -> Unit
            }
        }

        viewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.ivUserAvatar.setOnClickListener {
            (activity as? MainTabsActivity)?.selectProfileTab()
        }
        binding.btnFilter.setOnClickListener { showFilterMenu(it) }
        binding.btnAdd.setOnClickListener { showAddOptionsBottomSheet() }
        binding.btnMiniPlayerToggle.setOnClickListener {
            currentPreviewUrl?.let { previewAudioPlayer.toggle(it) }
        }
        binding.btnMiniPlayerClose.setOnClickListener {
            previewAudioPlayer.stop()
            currentPreviewUrl = null
            pendingSpotifyUrlForPreview = null
            stopMiniPlayer()
        }

        binding.swipeRefresh.setOnRefreshListener {
            scrollToTopOnNextRefresh = true
            viewModel.refresh()
        }
        // also ensure first load scrolls to top once data arrives
        scrollToTopOnNextRefresh = true
    }

    private fun setAvatarFromUrl(avatarUrl: String?) {
        val isValidUrl = !avatarUrl.isNullOrEmpty() &&
            !avatarUrl.contains("example.com") &&
            (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://"))
        if (isValidUrl) {
            Glide.with(this)
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

    private fun showFilterMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add("M?i ngu?i")
            menu.add("Ch? b?n bè")
            setOnMenuItemClickListener {
                binding.btnFilter.text = it.title
                viewModel.refresh()
                true
            }
            show()
        }
    }

    private fun showAddOptionsBottomSheet() {
        val bottomSheet = AddOptionsBottomSheet(
            onAddCollection = {
                addCollectionLauncher.launch(Intent(requireContext(), AddCollectionActivity::class.java))
            },
            onAddItem = {
                addItemLauncher.launch(Intent(requireContext(), AddItemActivity::class.java))
            }
        )
        bottomSheet.show(parentFragmentManager, AddOptionsBottomSheet.TAG)
    }

    private fun openComments(feedItem: FeedItem) {
        val displayName = feedItem.user.displayName ?: feedItem.user.username
        val subtitle = feedItem.item.metadata?.artist ?: feedItem.collection.name
        startActivity(Intent(requireContext(), CommentsActivity::class.java).apply {
            putExtra(CommentsActivity.EXTRA_TARGET_TYPE, "collection_item")
            putExtra(CommentsActivity.EXTRA_TARGET_ID, feedItem.id)
            putExtra(CommentsActivity.EXTRA_TITLE, feedItem.item.title)
            putExtra(CommentsActivity.EXTRA_POST_USER_NAME, displayName)
            putExtra(CommentsActivity.EXTRA_POST_USER_AVATAR, feedItem.user.avatarUrl)
            putExtra(CommentsActivity.EXTRA_POST_NOTE, feedItem.note ?: feedItem.item.description)
            putExtra(CommentsActivity.EXTRA_POST_ITEM_TITLE, feedItem.item.title)
            putExtra(CommentsActivity.EXTRA_POST_ITEM_SUBTITLE, subtitle)
            putExtra(CommentsActivity.EXTRA_POST_ITEM_COVER, feedItem.item.coverImageUrl)
            putExtra(CommentsActivity.EXTRA_POST_ADDED_AT, feedItem.addedAt)
        })
    }

    private fun openSharePost(feedItem: FeedItem) {
        val displayName = feedItem.user.displayName ?: feedItem.user.username
        val subtitle = feedItem.item.metadata?.artist ?: feedItem.collection.name
        startActivity(Intent(requireContext(), SharePostActivity::class.java).apply {
            putExtra(SharePostActivity.EXTRA_COLLECTION_ITEM_ID, feedItem.id)
            putExtra(SharePostActivity.EXTRA_ITEM_ID, feedItem.item.id)
            putExtra(SharePostActivity.EXTRA_ITEM_TITLE, feedItem.item.title)
            putExtra(SharePostActivity.EXTRA_ITEM_SUBTITLE, subtitle)
            putExtra(SharePostActivity.EXTRA_POST_NOTE, feedItem.note ?: feedItem.item.description)
            putExtra(SharePostActivity.EXTRA_COVER_URL, feedItem.item.coverImageUrl)
            putExtra(SharePostActivity.EXTRA_POST_USER_NAME, displayName)
            putExtra(SharePostActivity.EXTRA_POST_USER_AVATAR, feedItem.user.avatarUrl)
            putExtra(SharePostActivity.EXTRA_POST_ADDED_AT, feedItem.addedAt)
        })
    }
    private fun onUserClicked(userId: Int) {
        startActivity(Intent(requireContext(), UserProfileActivity::class.java).apply {
            putExtra(UserProfileActivity.EXTRA_USER_ID, userId)
        })
    }

    private fun showFeedItemMenu(feedItem: FeedItem, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add("Báo cáo bài vi?t")
            setOnMenuItemClickListener {
                showReportDialog("collection_item", feedItem.id, "Báo cáo bài vi?t")
                true
            }
            show()
        }
    }

    private fun showReportDialog(targetType: String, targetId: Int, title: String) {
        val reasonCodes = listOf(
            "spam" to "Spam",
            "harassment" to "Qu?y r?i",
            "hate_speech" to "Ngôn t? thù ghét",
            "nudity" to "N?i dung nh?y c?m",
            "misleading" to "Thông tin sai l?ch",
            "other" to "Lý do khác"
        )
        val labels = reasonCodes.map { it.second }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(labels) { _, which ->
                submitReport(targetType, targetId, reasonCodes[which].first, reasonCodes[which].second)
            }
            .setNegativeButton("H?y", null)
            .show()
    }

    private fun submitReport(targetType: String, targetId: Int, reasonCode: String, reasonLabel: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = "Bearer ${authPreferences.getToken().orEmpty()}"
                val response = RetrofitClient.apiService.createReport(
                    token = token,
                    body = mapOf(
                        "targetType" to targetType,
                        "targetId" to targetId,
                        "reasonCode" to reasonCode,
                        "reasonDetail" to reasonLabel
                    )
                )

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Ðã g?i báo cáo", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        response.errorBody()?.string() ?: "Không th? g?i báo cáo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "Không th? g?i báo cáo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private var pendingSpotifyUrlForPreview: String? = null
    private val miniPlayerUpdater = object : Runnable {
        override fun run() {
            val pos = previewAudioPlayer.getPositionMs()
            val dur = previewAudioPlayer.getDurationMs()
            binding.tvMiniPlayerTime.text = if (dur > 0) {
                "${formatTime(pos)}/${formatTime(dur)}"
            } else {
                "0:00/0:30"
            }
            binding.ivMiniPlayerPlay.setImageResource(
                if (previewAudioPlayer.isPlaying()) R.drawable.ic_pause_mini else R.drawable.ic_play_mini
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

        val query = listOfNotNull(feedItem.item.title, feedItem.item.metadata?.artist).joinToString(" ")
        viewLifecycleOwner.lifecycleScope.launch {
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
            } catch (_: Exception) {
                promptOpenSpotifyOrNotify(spotifyUrl)
            }
        }
    }

    private fun promptOpenSpotifyIfAvailable() {
        val url = pendingSpotifyUrlForPreview ?: return
        pendingSpotifyUrlForPreview = null
        promptOpenSpotifyOrNotify(url)
    }

    private fun promptOpenSpotifyOrNotify(spotifyUrl: String?) {
        if (spotifyUrl.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Không có ngu?n phát", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("H?t do?n xem tru?c")
            .setMessage("B?n mu?n m? Spotify d? nghe c? bài không?")
            .setPositiveButton("M? Spotify") { _, _ -> openSpotify(spotifyUrl) }
            .setNegativeButton("H?y", null)
            .show()
    }

    private fun openSpotify(spotifyUrl: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(spotifyUrl)).apply {
                setPackage("com.spotify.music")
            })
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(spotifyUrl)))
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Không th? m? Spotify", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMiniPlayer() {
        val title = currentPreviewTitle ?: "Xem tru?c"
        val artist = currentPreviewArtist
        binding.tvMiniPlayerTitle.text = if (!artist.isNullOrEmpty()) "$title • $artist" else title
        binding.tvMiniPlayerTitle.isSelected = true
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
        binding.tvMiniPlayerTitle.isSelected = false
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = (ms / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun handleLikeClick(feedItem: FeedItem, isLiked: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val token = "Bearer ${authPreferences.getToken()}"
                val body = mapOf("targetId" to feedItem.item.id, "targetType" to "item")
                if (isLiked) {
                    RetrofitClient.apiService.likeItem(token, body)
                } else {
                    RetrofitClient.apiService.unlikeItem(token, body)
                }
            } catch (_: Exception) {
                feedItem.isLiked = !isLiked
                feedAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Không th? c?p nh?t lu?t thích", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        binding.layoutMiniPlayer.removeCallbacks(miniPlayerUpdater)
        previewAudioPlayer.release()
        _binding = null
        super.onDestroyView()
    }
}





