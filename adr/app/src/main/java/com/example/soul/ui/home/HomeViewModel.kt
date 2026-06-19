package com.example.soul.ui.home

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.model.Collection
import com.example.soul.data.model.FeedItem
import com.example.soul.data.model.Profile
import com.example.soul.data.model.User
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.utils.Resource
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class HomeViewModel(
    private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
        private const val FEED_PAGE_SIZE = 10
    }

    private val authPreferences = AuthPreferences(context)
    private val apiService = RetrofitClient.apiService
    
    private val _profile = MutableLiveData<Resource<Profile>>()
    val profile: LiveData<Resource<Profile>> = _profile

    private val _collections = MutableLiveData<Resource<List<Collection>>>()
    val collections: LiveData<Resource<List<Collection>>> = _collections

    private val _feedItems = MutableLiveData<Resource<List<FeedItem>>>()
    val feedItems: LiveData<Resource<List<FeedItem>>> = _feedItems

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _sessionExpired = MutableLiveData<Boolean>()
    val sessionExpired: LiveData<Boolean> = _sessionExpired

    // null = "Mọi người" (mặc định), "friends" = "Chỉ bạn bè"
    private var feedScope: String? = null

    // Trạng thái phân trang feed (infinite scroll, mỗi trang FEED_PAGE_SIZE bài)
    private var feedPage = 1
    private var feedHasMore = true
    private var isLoadingMoreFeed = false
    private val accumulatedFeed = mutableListOf<FeedItem>()

    init {
        loadData()
    }

    fun loadData() {
        loadProfile()
        loadFeed()
        loadCollections()
    }

    fun refresh() {
        _isRefreshing.value = true
        loadData()
    }

    fun refreshProfileOnly() {
        loadProfile()
    }

    /** Đổi bộ lọc feed ("Mọi người" = null, "Chỉ bạn bè" = "friends") và tải lại feed. */
    fun setFeedScope(scope: String?) {
        if (feedScope == scope) return
        feedScope = scope
        _isRefreshing.value = true
        loadFeed()
    }

    /** Tải thêm trang feed kế tiếp khi người dùng cuộn xuống gần cuối. */
    fun loadMoreFeed() {
        if (isLoadingMoreFeed || !feedHasMore) return
        fetchFeed(reset = false)
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _profile.value = Resource.Loading()
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    _profile.value = Resource.Error("Bạn chưa đăng nhập")
                    return@launch
                }
                
                val response = apiService.getCurrentUser("Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    val userProfile = response.body()!!.user
                    if (userProfile != null) {
                        // Lưu lại user mới nhất để lần mở app kế tiếp có avatar ngay lập tức
                        authPreferences.saveUser(userProfile.toUser())
                        val profileData = Profile(
                            id = userProfile.id,
                            username = userProfile.username,
                            displayName = userProfile.displayName,
                            email = userProfile.email,
                            avatarUrl = userProfile.avatarUrl,
                            profileUrl = "shelf.im/${userProfile.username}",
                            bio = userProfile.bio,
                            role = userProfile.role,
                            accountStatus = userProfile.accountStatus
                        )
                        _profile.value = Resource.Success(profileData)
                    } else {
                        _profile.value = Resource.Error("Không thể tải hồ sơ")
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    _sessionExpired.value = true
                } else {
                    _profile.value = Resource.Error(
                        response.errorBody()?.string() ?: "Không thể tải hồ sơ"
                    )
                }
            } catch (e: SocketTimeoutException) {
                _profile.value = Resource.Error("Server đang khởi động...")
            } catch (e: ConnectException) {
                _profile.value = Resource.Error("Không thể kết nối server")
            } catch (e: UnknownHostException) {
                _profile.value = Resource.Error("Kiểm tra kết nối mạng")
            } catch (e: Exception) {
                _profile.value = Resource.Error(e.message ?: "Đã xảy ra lỗi mạng")
            }
        }
    }

    /** Tải lại feed từ đầu (trang 1). */
    private fun loadFeed() = fetchFeed(reset = true)

    /**
     * Lấy feed theo trang.
     * - reset = true: tải trang 1, thay toàn bộ danh sách (lần đầu / refresh / đổi bộ lọc).
     * - reset = false: tải trang kế tiếp và nối thêm vào danh sách hiện có (infinite scroll).
     */
    private fun fetchFeed(reset: Boolean) {
        viewModelScope.launch {
            val token = authPreferences.getToken()
            if (token.isNullOrEmpty()) {
                _feedItems.value = Resource.Error("Bạn chưa đăng nhập")
                _isRefreshing.value = false
                return@launch
            }

            if (reset) {
                feedPage = 1
                feedHasMore = true
                _feedItems.value = Resource.Loading()
            } else {
                isLoadingMoreFeed = true
            }

            try {
                val response = apiService.getFeed(
                    token = "Bearer $token",
                    page = feedPage,
                    limit = FEED_PAGE_SIZE,
                    scope = feedScope
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val pageItems = body.data
                    feedHasMore = body.pagination?.hasMore ?: false
                    Log.d(TAG, "Loaded ${pageItems.size} feed items (page $feedPage, hasMore=$feedHasMore)")

                    if (reset) {
                        accumulatedFeed.clear()
                    }
                    accumulatedFeed.addAll(pageItems)
                    // Phòng trùng lặp giữa các trang (mỗi bài là 1 collection_item id duy nhất)
                    val deduped = accumulatedFeed.distinctBy { it.id }
                    accumulatedFeed.clear()
                    accumulatedFeed.addAll(deduped)

                    _feedItems.value = Resource.Success(accumulatedFeed.toList())
                    feedPage += 1
                } else if (response.code() == 401 || response.code() == 403) {
                    _sessionExpired.value = true
                } else if (reset) {
                    _feedItems.value = Resource.Error(
                        response.errorBody()?.string() ?: "Failed to load feed"
                    )
                }
            } catch (e: SocketTimeoutException) {
                if (reset) _feedItems.value = Resource.Error("Server đang khởi động...")
            } catch (e: ConnectException) {
                if (reset) _feedItems.value = Resource.Error("Không thể kết nối server")
            } catch (e: UnknownHostException) {
                if (reset) _feedItems.value = Resource.Error("Kiểm tra kết nối mạng")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading feed", e)
                if (reset) _feedItems.value = Resource.Error(e.message ?: "Đã xảy ra lỗi mạng")
            } finally {
                _isRefreshing.value = false
                isLoadingMoreFeed = false
            }
        }
    }

    private fun loadCollections() {
        viewModelScope.launch {
            _collections.value = Resource.Loading()
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    _collections.value = Resource.Error("Bạn chưa đăng nhập")
                    _isRefreshing.value = false
                    return@launch
                }
                
                val currentUserId = authPreferences.getUser()?.id
                val response = apiService.getCollections(
                    token = "Bearer $token",
                    userId = currentUserId,
                    limit = 20
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val collections = response.body()!!.data
                    Log.d(TAG, "Loaded ${collections.size} collections")
                    collections.forEach { c ->
                        Log.d(TAG, "Collection: ${c.name}, cover: ${c.coverImageUrl}")
                    }
                    _collections.value = Resource.Success(collections)
                } else if (response.code() == 401 || response.code() == 403) {
                    _sessionExpired.value = true
                } else {
                    _collections.value = Resource.Error(
                        response.errorBody()?.string() ?: "Failed to load collections"
                    )
                }
            } catch (e: SocketTimeoutException) {
                _collections.value = Resource.Error("Server đang khởi động...")
            } catch (e: ConnectException) {
                _collections.value = Resource.Error("Không thể kết nối server")
            } catch (e: UnknownHostException) {
                _collections.value = Resource.Error("Kiểm tra kết nối mạng")
            } catch (e: Exception) {
                _collections.value = Resource.Error(e.message ?: "Đã xảy ra lỗi mạng")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun logout() {
        authPreferences.clearSession()
    }

    private fun com.example.soul.data.model.UserProfile.toUser(): User {
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


