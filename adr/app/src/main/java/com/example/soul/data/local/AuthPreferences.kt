package com.example.soul.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.soul.data.model.User
import com.google.gson.Gson

/**
 * SharedPreferences helper class to store authentication data
 */
class AuthPreferences(context: Context) {
    
    companion object {
        private const val PREF_NAME = "soul_auth_prefs"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_data"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * Save authentication token
     */
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }
    
    /**
     * Get saved token
     */
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }
    
    /**
     * Save user data
     */
    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER, userJson).apply()
    }
    
    /**
     * Get saved user data
     */
    fun getUser(): User? {
        val userJson = prefs.getString(KEY_USER, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Set login status
     */
    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getToken() != null
    }
    
    /**
     * Save login session (token + user + status)
     */
    fun saveLoginSession(token: String, user: User) {
        saveToken(token)
        saveUser(user)
        setLoggedIn(true)
    }
    
    /**
     * Clear all auth data (logout)
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
