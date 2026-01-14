package com.example.soul.data.model

import com.google.gson.annotations.SerializedName

/**
 * Health check response model
 */
data class HealthResponse(
    @SerializedName("status") 
    val status: String? = null,
    
    @SerializedName("database") 
    val database: Any? = null,
    
    @SerializedName("timestamp") 
    val timestamp: String? = null,
    
    @SerializedName("message") 
    val message: String? = null,
    
    @SerializedName("error") 
    val error: String? = null
) {
    fun getDatabaseStatus(): String {
        return when (database) {
            is String -> database as String
            is Boolean -> if (database as Boolean) "Connected" else "Disconnected"
            is Map<*, *> -> (database as? Map<*, *>)?.get("status")?.toString() ?: "Unknown"
            else -> database?.toString() ?: "N/A"
        }
    }

    fun getDisplayText(): String {
        return """
            Status: ${status ?: "N/A"}
            Database: ${getDatabaseStatus()}
            Message: ${message ?: error ?: "No message"}
            Timestamp: ${timestamp ?: "N/A"}
        """.trimIndent()
    }
}
