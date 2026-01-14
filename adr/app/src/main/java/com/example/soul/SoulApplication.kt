package com.example.soul

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class SoulApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Force Light Mode - prevent dark mode from breaking UI
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
