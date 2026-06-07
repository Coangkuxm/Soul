package com.example.soul.utils

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.soul.data.local.AuthPreferences
import com.example.soul.data.remote.ChatSocketManager
import com.example.soul.ui.auth.LoginActivity

object SessionManager {

    const val EXTRA_LOGOUT_REASON = "logout_reason"
    const val REASON_UNAUTHORIZED = "unauthorized"
    const val REASON_ACCOUNT_LOCKED = "account_locked"

    @Volatile
    private var lastHandledAt: Long = 0L

    fun handleUnauthorized(context: Context, reason: String = REASON_UNAUTHORIZED) {
        val now = System.currentTimeMillis()
        if (now - lastHandledAt < 1200L) return
        lastHandledAt = now

        val appContext = context.applicationContext
        AuthPreferences(appContext).clearSession()
        ChatSocketManager.disconnect()

        Handler(Looper.getMainLooper()).post {
            val intent = Intent(appContext, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(EXTRA_LOGOUT_REASON, reason)
            }
            appContext.startActivity(intent)
        }
    }
}
