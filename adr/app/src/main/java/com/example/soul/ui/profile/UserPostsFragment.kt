package com.example.soul.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.soul.R
import com.example.soul.audio.PreviewAudioPlayer
import com.example.soul.audio.PreviewResolver
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.FeedItem
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.databinding.FragmentUserPostsBinding
import com.example.soul.ui.comments.CommentsActivity
import com.example.soul.ui.home.adapter.FeedAdapter
import com.example.soul.ui.messenger.SharePostBottomSheet
import kotlinx.coroutines.launch

/**
 * Tab "Bài đăng" trên trang cá nhân: hiển thị các item một người dùng đã đăng,
 * dùng chung [FeedAdapter] với trang chủ (thích, bình luận, phát nhạc, chia sẻ, báo cáo).
 */
class UserPostsFragment : Fragment() {

    companion object {
        private const val ARG_USER_ID = "arg_user_id"
        private const val PAGE_SIZE = 20

        fun newInstance(userId: Int): UserPostsFragment = UserPostsFragment().apply {
            arguments = Bundle().apply { putInt(ARG_USER_ID, userId) }
        }
    }

    private var _binding: FragmentUserPostsBinding? = null
    private val binding get() = _binding!!

    private lateinit var authPreferences: AuthPreferences
    private lateinit var feedAdapter: FeedAdapter
    private lateinit var previewAudioPlayer: PreviewAudioPlayer

    private var userId: Int = -1

    private val posts = mutableListOf<FeedItem>()
    private var page = 1
    private var isLoadingPage = false
    private var hasMore = true

    private var currentPreviewUrl: String? = null
    private var currentPreviewTitle: String? = null
    private var currentPreviewArtist: String? = null
    private var currentPreviewCoverUrl: String? = null
    private var pendingSpotifyUrlForPreview: String? = null

    private val commentsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != android.app.Activity.RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val targetId = data.getIntExtra(CommentsActivity.EXTRA_TARGET_ID, -1)
        val count = data.getIntExtra(CommentsActivity.EXTRA_RESULT_COMMENT_COUNT, -1)
        if (targetId != -1 && count >= 0) {
            val item = feedAdapter.currentList.firstOrNull { it.id == targetId }
            if (item != null && item.commentCount != count) {
                item.commentCount = count
                feedAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userId = arguments?.getInt(ARG_USER_ID, -1) ?: -1
        authPreferences = AuthPreferences(requireContext())
        previewAudioPlayer = PreviewAudioPlayer(requireContext())

        binding.swipeRefresh.setColorSchemeResources(R.color.primary, R.color.primary_dark)
        setupRecyclerView()
        setupMiniPlayer()
        binding.swipeRefresh.setOnRefreshListener { loadPosts(reset = true) }
        loadPosts(reset = true)

        // Hâm nóng kết nối Deezer để lần bấm play đầu tiên không bị trượt.
        lifecycleScope.launch { PreviewResolver.warmUp() }
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
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0) return
                    val lm = rv.layoutManager as LinearLayoutManager
                    // Sắp chạm đáy -> tải thêm trang kế tiếp.
                    if (!isLoadingPage && hasMore &&
                        lm.findLastVisibleItemPosition() >= lm.itemCount - 3
                    ) {
                        loadPosts(reset = false)
                    }
                }
            })
        }
    }

    private fun loadPosts(reset: Boolean) {
        if (userId <= 0 || isLoadingPage) return
        if (!reset && !hasMore) return
        isLoadingPage = true
        if (reset) {
            page = 1
            hasMore = true
        }
        lifecycleScope.launch {
            if (reset && posts.isEmpty()) binding.progressBar.visibility = View.VISIBLE
            try {
                val token = "Bearer ${authPreferences.getToken().orEmpty()}"
                val response = RetrofitClient.apiService.getUserPosts(token, userId, page = page, limit = PAGE_SIZE)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val newItems = body.data
                    if (reset) posts.clear()
                    val existingIds = posts.map { it.id }.toHashSet()
                    posts.addAll(newItems.filter { it.id !in existingIds })
                    hasMore = body.pagination?.hasMore ?: (newItems.size >= PAGE_SIZE)
                    if (newItems.isNotEmpty()) page++

                    feedAdapter.submitList(posts.toList())
                    val empty = posts.isEmpty()
                    binding.layoutEmpty.visibility = if (empty) View.VISIBLE else View.GONE
                    binding.rvFeed.visibility = if (empty) View.GONE else View.VISIBLE
                } else if (reset) {
                    showEmptyWithError(response.errorBody()?.string() ?: "Không tải được bài đăng")
                }
            } catch (e: Exception) {
                if (reset) showEmptyWithError(e.message ?: "Lỗi mạng")
            } finally {
                isLoadingPage = false
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun showEmptyWithError(message: String) {
        if (posts.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvFeed.visibility = View.GONE
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    fun refresh() {
        if (_binding != null) loadPosts(reset = true)
    }

    private fun onUserClicked(targetUserId: Int) {
        // Đang ở trang cá nhân của chính người này thì không mở lại.
        if (targetUserId == userId) return
        startActivity(Intent(requireContext(), UserProfileActivity::class.java).apply {
            putExtra(UserProfileActivity.EXTRA_USER_ID, targetUserId)
        })
    }

    private fun openComments(feedItem: FeedItem) {
        val displayName = feedItem.user.displayName ?: feedItem.user.username
        val subtitle = feedItem.item.metadata?.artist ?: feedItem.collection.name
        commentsLauncher.launch(Intent(requireContext(), CommentsActivity::class.java).apply {
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
        SharePostBottomSheet.newInstance(
            collectionItemId = feedItem.id,
            itemId = feedItem.item.id,
            itemTitle = feedItem.item.title,
            itemSubtitle = subtitle,
            postNote = feedItem.note ?: feedItem.item.description,
            coverUrl = feedItem.item.coverImageUrl,
            postUserName = displayName,
            postUserAvatar = feedItem.user.avatarUrl,
            postAddedAt = feedItem.addedAt
        ).show(parentFragmentManager, SharePostBottomSheet.TAG)
    }

    private fun handleLikeClick(feedItem: FeedItem, isLiked: Boolean) {
        lifecycleScope.launch {
            try {
                val token = "Bearer ${authPreferences.getToken()}"
                val body = mapOf(
                    "targetId" to feedItem.item.id,
                    "targetType" to "item",
                    "collectionItemId" to feedItem.id
                )
                if (isLiked) {
                    RetrofitClient.apiService.likeItem(token, body)
                } else {
                    RetrofitClient.apiService.unlikeItem(token, body)
                }
            } catch (_: Exception) {
                feedItem.isLiked = !isLiked
                feedItem.likeCount = (feedItem.likeCount + if (isLiked) -1 else 1).coerceAtLeast(0)
                feedAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), "Không thể cập nhật lượt thích", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFeedItemMenu(feedItem: FeedItem, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menu.add("Báo cáo bài viết")
            setOnMenuItemClickListener {
                showReportDialog("collection_item", feedItem.id)
                true
            }
            show()
        }
    }

    private fun showReportDialog(targetType: String, targetId: Int) {
        val reasonCodes = listOf(
            "spam" to "Spam",
            "harassment" to "Quấy rối",
            "hate_speech" to "Ngôn từ thù ghét",
            "nudity" to "Nội dung nhạy cảm",
            "misleading" to "Thông tin sai lệch",
            "other" to "Lý do khác"
        )
        val labels = reasonCodes.map { it.second }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Báo cáo bài viết")
            .setItems(labels) { _, which ->
                submitReport(targetType, targetId, reasonCodes[which].first, reasonCodes[which].second)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun submitReport(targetType: String, targetId: Int, reasonCode: String, reasonLabel: String) {
        lifecycleScope.launch {
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
                Toast.makeText(
                    requireContext(),
                    if (response.isSuccessful) "Đã gửi báo cáo" else "Không thể gửi báo cáo",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "Không thể gửi báo cáo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ==================== Mini player nhạc ====================

    private val miniPlayerUpdater = object : Runnable {
        override fun run() {
            val pos = previewAudioPlayer.getPositionMs()
            val dur = previewAudioPlayer.getDurationMs()
            binding.tvMiniPlayerTime.text = if (dur > 0) "${formatTime(pos)}/${formatTime(dur)}" else "0:00/0:30"
            binding.ivMiniPlayerPlay.setImageResource(
                if (previewAudioPlayer.isPlaying()) R.drawable.ic_pause_mini else R.drawable.ic_play_mini
            )
            binding.layoutMiniPlayer.postDelayed(this, 500)
        }
    }

    private fun setupMiniPlayer() {
        binding.btnMiniPlayerToggle.setOnClickListener {
            currentPreviewUrl?.let { previewAudioPlayer.toggle(it) }
        }
        binding.btnMiniPlayerClose.setOnClickListener {
            previewAudioPlayer.stop()
            currentPreviewUrl = null
            pendingSpotifyUrlForPreview = null
            stopMiniPlayer()
        }
    }

    private fun handlePlayClick(feedItem: FeedItem) {
        currentPreviewTitle = feedItem.item.title
        currentPreviewArtist = feedItem.item.metadata?.artist
        currentPreviewCoverUrl = feedItem.item.coverImageUrl
        val spotifyUrl = feedItem.item.metadata?.spotifyUrl

        lifecycleScope.launch {
            val previewUrl = PreviewResolver.resolve(
                feedItem.item.title,
                feedItem.item.metadata?.artist,
                feedItem.item.metadata?.previewUrl
            )
            if (previewUrl != null) {
                pendingSpotifyUrlForPreview = spotifyUrl
                previewAudioPlayer.setOnEndedListener {
                    stopMiniPlayer()
                    promptOpenSpotifyIfAvailable()
                }
                startMiniPlayer()
                currentPreviewUrl = previewUrl
                previewAudioPlayer.toggle(previewUrl)
            } else {
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
            Toast.makeText(requireContext(), "Không có nguồn phát", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Hết đoạn xem trước")
            .setMessage("Bạn muốn mở Spotify để nghe cả bài không?")
            .setPositiveButton("Mở Spotify") { _, _ -> openSpotify(spotifyUrl) }
            .setNegativeButton("Hủy", null)
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
                Toast.makeText(requireContext(), "Không thể mở Spotify", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMiniPlayer() {
        val title = currentPreviewTitle ?: "Xem trước"
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

    override fun onDestroyView() {
        binding.layoutMiniPlayer.removeCallbacks(miniPlayerUpdater)
        previewAudioPlayer.release()
        _binding = null
        super.onDestroyView()
    }
}
