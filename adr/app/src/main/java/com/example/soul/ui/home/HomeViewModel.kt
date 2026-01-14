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

    init {
        loadData()
    }

    fun loadData() {
        loadProfile()
        loadFeed()
    }

    fun refresh() {
        _isRefreshing.value = true
        loadData()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _profile.value = Resource.Loading()
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    _profile.value = Resource.Error("Not logged in")
                    return@launch
                }
                
                val response = apiService.getCurrentUser("Bearer $token")
                
                if (response.isSuccessful && response.body() != null) {
                    val userProfile = response.body()!!.user
                    if (userProfile != null) {
                        val profileData = Profile(
                            id = userProfile.id,
                            username = userProfile.username,
                            email = userProfile.email,
                            avatarUrl = userProfile.avatarUrl,
                            profileUrl = "shelf.im/${userProfile.username}",
                            bio = userProfile.bio
                        )
                        _profile.value = Resource.Success(profileData)
                    } else {
                        _profile.value = Resource.Error("Failed to load profile")
                    }
                } else {
                    _profile.value = Resource.Error(
                        response.errorBody()?.string() ?: "Failed to load profile"
                    )
                }
            } catch (e: SocketTimeoutException) {
                _profile.value = Resource.Error("⏳ Server đang khởi động...")
            } catch (e: ConnectException) {
                _profile.value = Resource.Error("❌ Không thể kết nối server")
            } catch (e: UnknownHostException) {
                _profile.value = Resource.Error("❌ Kiểm tra kết nối mạng")
            } catch (e: Exception) {
                _profile.value = Resource.Error(e.message ?: "Network error occurred")
            }
        }
    }

    private fun loadFeed() {
        viewModelScope.launch {
            _feedItems.value = Resource.Loading()
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    _feedItems.value = Resource.Error("Not logged in")
                    _isRefreshing.value = false
                    return@launch
                }
                
                val response = apiService.getFeed(
                    token = "Bearer $token",
                    page = 1,
                    limit = 20
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val feedData = response.body()!!.data
                    Log.d(TAG, "Loaded ${feedData.size} feed items")
                    feedData.forEach { item ->
                        Log.d(TAG, "Feed item: ${item.item.title} by ${item.user.username}")
                    }
                    _feedItems.value = Resource.Success(feedData)
                } else {
                    _feedItems.value = Resource.Error(
                        response.errorBody()?.string() ?: "Failed to load feed"
                    )
                }
            } catch (e: SocketTimeoutException) {
                _feedItems.value = Resource.Error("⏳ Server đang khởi động...")
            } catch (e: ConnectException) {
                _feedItems.value = Resource.Error("❌ Không thể kết nối server")
            } catch (e: UnknownHostException) {
                _feedItems.value = Resource.Error("❌ Kiểm tra kết nối mạng")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading feed", e)
                _feedItems.value = Resource.Error(e.message ?: "Network error occurred")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun loadCollections() {
        viewModelScope.launch {
            _collections.value = Resource.Loading()
            try {
                val token = authPreferences.getToken()
                if (token.isNullOrEmpty()) {
                    _collections.value = Resource.Error("Not logged in")
                    _isRefreshing.value = false
                    return@launch
                }
                
                val response = apiService.getCollections(
                    token = "Bearer $token",
                    limit = 20
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val collections = response.body()!!.data
                    Log.d(TAG, "Loaded ${collections.size} collections")
                    collections.forEach { c ->
                        Log.d(TAG, "Collection: ${c.name}, cover: ${c.coverImageUrl}")
                    }
                    _collections.value = Resource.Success(collections)
                } else {
                    _collections.value = Resource.Error(
                        response.errorBody()?.string() ?: "Failed to load collections"
                    )
                }
            } catch (e: SocketTimeoutException) {
                _collections.value = Resource.Error("⏳ Server đang khởi động...")
            } catch (e: ConnectException) {
                _collections.value = Resource.Error("❌ Không thể kết nối server")
            } catch (e: UnknownHostException) {
                _collections.value = Resource.Error("❌ Kiểm tra kết nối mạng")
            } catch (e: Exception) {
                _collections.value = Resource.Error(e.message ?: "Network error occurred")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun logout() {
        authPreferences.clearSession()
    }
}
