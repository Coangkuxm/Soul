package com.example.soul

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class SoulApplication : Application() {

    companion object {
        lateinit var app: SoulApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        app = this
        
        // Force Light Mode - prevent dark mode from breaking UI
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
