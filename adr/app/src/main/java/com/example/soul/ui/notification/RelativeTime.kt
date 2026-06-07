package com.example.soul.ui.notification

import java.time.Duration
import java.time.OffsetDateTime

object RelativeTime {
    fun format(isoTime: String?): String {
        if (isoTime.isNullOrEmpty()) return ""
        return try {
            val created = OffsetDateTime.parse(isoTime).toInstant()
            val now = java.time.Instant.now()
            val sec = Duration.between(created, now).seconds.coerceAtLeast(0)
            when {
                sec < 60 -> "vừa xong"
                sec < 3600 -> "${sec / 60} phút trước"
                sec < 86400 -> "${sec / 3600} giờ trước"
                else -> "${sec / 86400} ngày trước"
            }
        } catch (_: Exception) {
            isoTime
        }
    }
}
