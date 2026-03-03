package com.example.soul.data.remote

import android.util.Log
import com.example.soul.data.model.ChatConversationReadEvent
import com.example.soul.data.model.ChatConversationUpdatedEvent
import com.example.soul.data.model.ChatMessage
import com.google.gson.Gson
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URLEncoder

object ChatSocketManager {

    private const val TAG = "ChatSocketManager"
    private val gson = Gson()

    private var socket: Socket? = null
    private var tokenRef: String? = null

    private val messageListeners = mutableSetOf<(ChatMessage) -> Unit>()
    private val conversationUpdatedListeners = mutableSetOf<(ChatConversationUpdatedEvent) -> Unit>()
    private val conversationReadListeners = mutableSetOf<(ChatConversationReadEvent) -> Unit>()

    fun connect(token: String) {
        if (token.isBlank()) return
        if (socket?.connected() == true && token == tokenRef) return

        disconnect()

        val encodedToken = URLEncoder.encode(token, "UTF-8")
        val options = IO.Options().apply {
            forceNew = true
            reconnection = true
            query = "token=$encodedToken"
            extraHeaders = mapOf("Authorization" to listOf("Bearer $token"))
        }

        val endpoint = RetrofitClient.BASE_URL.trimEnd('/')
        socket = IO.socket(endpoint, options).apply {
            on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket connected")
            }
            on(Socket.EVENT_DISCONNECT) { args ->
                val reason = args.firstOrNull()?.toString().orEmpty()
                Log.d(TAG, "Socket disconnected: $reason")
            }
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = args.firstOrNull()?.toString().orEmpty()
                Log.e(TAG, "Socket connect error: $error")
            }
            on("message:new") { args ->
                parseEvent<ChatMessage>(args.firstOrNull())?.let { message ->
                    messageListeners.toList().forEach { it.invoke(message) }
                }
            }
            on("conversation:updated") { args ->
                parseEvent<ChatConversationUpdatedEvent>(args.firstOrNull())?.let { event ->
                    conversationUpdatedListeners.toList().forEach { it.invoke(event) }
                }
            }
            on("conversation:read") { args ->
                parseEvent<ChatConversationReadEvent>(args.firstOrNull())?.let { event ->
                    conversationReadListeners.toList().forEach { it.invoke(event) }
                }
            }
            connect()
        }

        tokenRef = token
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        tokenRef = null
    }

    fun joinConversation(conversationId: Int, callback: ((Boolean, String?) -> Unit)? = null) {
        if (conversationId <= 0) {
            callback?.invoke(false, "Invalid conversation id")
            return
        }
        emitWithAck("conversation:join", mapOf("conversationId" to conversationId), callback)
    }

    fun leaveConversation(conversationId: Int) {
        if (conversationId <= 0) return
        socket?.emit("conversation:leave", mapOf("conversationId" to conversationId))
    }

    fun sendMessage(conversationId: Int, content: String, callback: ((Boolean, String?) -> Unit)? = null) {
        if (conversationId <= 0 || content.isBlank()) {
            callback?.invoke(false, "Message cannot be empty")
            return
        }
        emitWithAck(
            event = "message:send",
            payload = mapOf(
                "conversationId" to conversationId,
                "content" to content.trim()
            ),
            callback = callback
        )
    }

    fun markConversationRead(conversationId: Int, callback: ((Boolean, String?) -> Unit)? = null) {
        if (conversationId <= 0) return
        emitWithAck("conversation:read", mapOf("conversationId" to conversationId), callback)
    }

    fun addMessageListener(listener: (ChatMessage) -> Unit) {
        messageListeners.add(listener)
    }

    fun removeMessageListener(listener: (ChatMessage) -> Unit) {
        messageListeners.remove(listener)
    }

    fun addConversationUpdatedListener(listener: (ChatConversationUpdatedEvent) -> Unit) {
        conversationUpdatedListeners.add(listener)
    }

    fun removeConversationUpdatedListener(listener: (ChatConversationUpdatedEvent) -> Unit) {
        conversationUpdatedListeners.remove(listener)
    }

    fun addConversationReadListener(listener: (ChatConversationReadEvent) -> Unit) {
        conversationReadListeners.add(listener)
    }

    fun removeConversationReadListener(listener: (ChatConversationReadEvent) -> Unit) {
        conversationReadListeners.remove(listener)
    }

    private inline fun <reified T> parseEvent(payload: Any?): T? {
        return try {
            when (payload) {
                null -> null
                is String -> gson.fromJson(payload, T::class.java)
                else -> gson.fromJson(payload.toString(), T::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to parse event payload", e)
            null
        }
    }

    private fun emitWithAck(
        event: String,
        payload: Any,
        callback: ((Boolean, String?) -> Unit)?
    ) {
        val current = socket
        if (current == null) {
            callback?.invoke(false, "Socket is not connected")
            return
        }

        current.emit(event, payload, object : Ack {
            override fun call(vararg args: Any?) {
                val ackPayload = args.firstOrNull()
                val success = extractBoolean(ackPayload, "success")
                val error = extractString(ackPayload, "error")
                callback?.invoke(success, error)
            }
        })
    }

    private fun extractBoolean(payload: Any?, key: String): Boolean {
        return try {
            val map = gson.fromJson(payload?.toString().orEmpty(), Map::class.java)
            (map[key] as? Boolean) ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun extractString(payload: Any?, key: String): String? {
        return try {
            val map = gson.fromJson(payload?.toString().orEmpty(), Map::class.java)
            (map[key] as? String)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
