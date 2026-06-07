package com.example.soul.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soul.data.model.HealthResponse
import com.example.soul.data.model.User
import com.example.soul.data.repository.AuthRepository
import com.example.soul.data.repository.MainRepository
import com.example.soul.utils.Resource
import kotlinx.coroutines.launch

/**
 * ViewModel for Main screen
 */
class MainViewModel(
    private val authRepository: AuthRepository,
    private val mainRepository: MainRepository
) : ViewModel() {

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _connectionState = MutableLiveData<Resource<HealthResponse>>()
    val connectionState: LiveData<Resource<HealthResponse>> = _connectionState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        _currentUser.value = authRepository.getCurrentUser()
    }

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun getToken(): String? = authRepository.getToken()

    fun testConnection() {
        viewModelScope.launch {
            _isLoading.value = true
            _connectionState.value = Resource.Loading()
            val result = mainRepository.testConnection()
            _isLoading.value = false
            _connectionState.value = result
        }
    }

    fun logout() {
        authRepository.logout()
        _logoutEvent.value = true
    }

    fun onLogoutHandled() {
        _logoutEvent.value = false
    }

    fun getUserInfoDisplay(): String {
        val user = _currentUser.value ?: return "Chưa có dữ liệu người dùng"

        return buildString {
            appendLine("Tên người dùng: ${user.username}")
            appendLine("Email: ${user.email}")
            appendLine("Tên hiển thị: ${user.displayName ?: "Chưa đặt"}")
            appendLine("Tiểu sử: ${user.bio ?: "Chưa có"}")
            appendLine("--------------------")
            appendLine("Người theo dõi: ${user.followerCount}")
            appendLine("Đang theo dõi: ${user.followingCount}")
            appendLine("Bộ sưu tập: ${user.collectionCount}")
        }
    }

    fun getWelcomeMessage(): String {
        val user = _currentUser.value
        val displayName = user?.displayName ?: user?.username ?: "bạn"
        return "Xin chào, $displayName!"
    }
}
