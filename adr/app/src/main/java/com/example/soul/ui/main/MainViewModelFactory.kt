package com.example.soul.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.RetrofitClient
import com.example.soul.data.repository.AuthRepository
import com.example.soul.data.repository.MainRepository

/**
 * Factory for creating MainViewModel with dependencies
 */
class MainViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val authPreferences = AuthPreferences(context)
            val apiService = RetrofitClient.apiService
            val authRepository = AuthRepository(apiService, authPreferences)
            val mainRepository = MainRepository(apiService)
            
            return MainViewModel(authRepository, mainRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
