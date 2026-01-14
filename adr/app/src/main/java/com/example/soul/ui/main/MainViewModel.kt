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
    
    // Current user
    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser
    
    // Server connection state
    private val _connectionState = MutableLiveData<Resource<HealthResponse>>()
    val connectionState: LiveData<Resource<HealthResponse>> = _connectionState
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Logout event
    private val _logoutEvent = MutableLiveData<Boolean>()
    val logoutEvent: LiveData<Boolean> = _logoutEvent
    
    init {
        loadCurrentUser()
    }
    
    /**
     * Load current logged in user
     */
    fun loadCurrentUser() {
        _currentUser.value = authRepository.getCurrentUser()
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
    
    /**
     * Get auth token
     */
    fun getToken(): String? {
        return authRepository.getToken()
    }
    
    /**
     * Test server connection
     */
    fun testConnection() {
        viewModelScope.launch {
            _isLoading.value = true
            _connectionState.value = Resource.Loading()
            
            val result = mainRepository.testConnection()
            
            _isLoading.value = false
            _connectionState.value = result
        }
    }
    
    /**
     * Logout user
     */
    fun logout() {
        authRepository.logout()
        _logoutEvent.value = true
    }
    
    /**
     * Reset logout event after handling
     */
    fun onLogoutHandled() {
        _logoutEvent.value = false
    }
    
    /**
     * Build user info display string
     */
    fun getUserInfoDisplay(): String {
        val user = _currentUser.value ?: return "No user data"
        
        return buildString {
            appendLine("📛 Username: ${user.username}")
            appendLine("📧 Email: ${user.email}")
            appendLine("🏷️ Display Name: ${user.displayName ?: "Chưa đặt"}")
            appendLine("📝 Bio: ${user.bio ?: "Chưa có"}")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("👥 Followers: ${user.followerCount}")
            appendLine("👁️ Following: ${user.followingCount}")
            appendLine("📚 Collections: ${user.collectionCount}")
        }
    }
    
    /**
     * Get welcome message
     */
    fun getWelcomeMessage(): String {
        val user = _currentUser.value
        val displayName = user?.displayName ?: user?.username ?: "User"
        return "Xin chào, $displayName! 👋"
    }
}
