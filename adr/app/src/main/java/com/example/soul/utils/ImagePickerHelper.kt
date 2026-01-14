package com.example.soul.utils

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Helper class for image processing
 */
object ImagePickerHelper {
    
    /**
     * Convert Uri to Base64 string for upload
     */
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            bytes?.let { Base64.encodeToString(it, Base64.DEFAULT) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get file size from Uri in MB
     */
    fun getFileSizeMB(context: Context, uri: Uri): Double {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.available() ?: 0
            inputStream?.close()
            bytes / (1024.0 * 1024.0)
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * Check if file size is within limit (default 5MB)
     */
    fun isFileSizeValid(context: Context, uri: Uri, maxSizeMB: Double = 5.0): Boolean {
        return getFileSizeMB(context, uri) <= maxSizeMB
    }
}
