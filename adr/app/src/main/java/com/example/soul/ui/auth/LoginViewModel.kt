package com.example.soul.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.soul.data.model.User
import com.example.soul.data.model.auth.LoginResponse
import com.example.soul.data.repository.AuthRepository
import com.example.soul.utils.Resource
import kotlinx.coroutines.launch

/**
 * ViewModel for Login screen
 * Handles login logic and UI state
 */
class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // Login state
    private val _loginState = MutableLiveData<Resource<LoginResponse>>()
    val loginState: LiveData<Resource<LoginResponse>> = _loginState
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Validation errors
    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError
    
    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError
    
    /**
     * Check if user is already logged in
     */
    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
    
    /**
     * Validate email input
     */
    fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                _emailError.value = "Email không được để trống"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _emailError.value = "Email không hợp lệ"
                false
            }
            else -> {
                _emailError.value = null
                true
            }
        }
    }
    
    /**
     * Validate password input
     */
    fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                _passwordError.value = "Mật khẩu không được để trống"
                false
            }
            password.length < 6 -> {
                _passwordError.value = "Mật khẩu phải có ít nhất 6 ký tự"
                false
            }
            else -> {
                _passwordError.value = null
                true
            }
        }
    }
    
    /**
     * Validate all inputs
     */
    fun validateInputs(email: String, password: String): Boolean {
        val isEmailValid = validateEmail(email)
        val isPasswordValid = validatePassword(password)
        return isEmailValid && isPasswordValid
    }
    
    /**
     * Perform login
     */
    fun login(email: String, password: String) {
        if (!validateInputs(email, password)) {
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _loginState.value = Resource.Loading()
            
            val result = authRepository.login(email, password)
            
            _isLoading.value = false
            _loginState.value = result
        }
    }
    
    /**
     * Get logged in user
     */
    fun getUser(): User? {
        return authRepository.getCurrentUser()
    }
}
