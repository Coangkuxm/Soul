package com.example.soul.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
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
     * Get file size from Uri in MB.
     * Đọc kích thước thật qua OpenableColumns.SIZE (InputStream.available() KHÔNG phải
     * kích thước file). Fallback đọc toàn bộ stream nếu provider không trả về SIZE.
     */
    fun getFileSizeMB(context: Context, uri: Uri): Double {
        val bytes = getFileSizeBytes(context, uri)
        return bytes / (1024.0 * 1024.0)
    }

    private fun getFileSizeBytes(context: Context, uri: Uri): Long {
        // 1) Ưu tiên metadata SIZE từ ContentResolver
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        val size = cursor.getLong(sizeIndex)
                        if (size > 0) return size
                    }
                }
            }
        } catch (_: Exception) {
            // bỏ qua, thử cách dưới
        }

        // 2) Fallback: đếm số byte thực tế khi đọc stream
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                var total = 0L
                val buffer = ByteArray(8 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    total += read
                }
                total
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Check if file size is within limit (default 5MB)
     */
    fun isFileSizeValid(context: Context, uri: Uri, maxSizeMB: Double = 5.0): Boolean {
        return getFileSizeMB(context, uri) <= maxSizeMB
    }
}
