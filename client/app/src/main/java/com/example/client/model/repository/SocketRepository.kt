package com.example.client.model.repository

import com.example.client.model.data.ChatRoom
import com.example.client.model.data.Message
import com.example.client.model.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

/**
 * Minimal stub implementation so ViewModel compiles.
 * Replace with the real socket repository implementation later.
 */
class SocketRepository {

    // Public flows expected by ChatViewModel
    private val _users = MutableStateFlow<List<User>>(emptyList())
    private val _rooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    private val _messagesByRoom = MutableStateFlow<Map<String, List<Message>>>(emptyMap())

    val users: StateFlow<List<User>> = _users
    val rooms: StateFlow<List<ChatRoom>> = _rooms
    val messagesByRoom: StateFlow<Map<String, List<Message>>> = _messagesByRoom

    // Simple in-process socket replacement with basic on/emit behavior
    val socket: SimpleSocket = SimpleSocket()

    fun connect(userId: String) {
        // no-op for stub
    }

    fun disconnect() {
        // no-op for stub
    }

    fun requestUsers() {
        // no-op; real implementation should update `users` flow
    }

    fun requestRooms(userId: String) {
        // no-op; real implementation should update `rooms` flow
    }

    fun joinRoom(roomId: String) {
        // no-op
    }

    fun syncMessages(roomId: String) {
        // no-op; real implementation should update `messagesByRoom`
    }

    fun sendStopTyping(roomId: String) {
        socket.emit("stop_typing", roomId)
    }

    fun ensurePrivateRoom(currentUserId: String, user: User): ChatRoom {
        val id = "priv_${listOf(currentUserId, user.id).sorted().joinToString("_") }"
        val name = user.fullName.ifBlank { user.username }
        val room = ChatRoom(id = id, name = name)
        return room
    }

    fun createGroup(name: String, memberIds: List<String>, currentUserId: String): ChatRoom {
        val id = UUID.randomUUID().toString()
        val room = ChatRoom(id = id, name = name)
        return room
    }

    fun addMember(roomId: String, userId: String) { /* no-op */ }
    fun kickMember(roomId: String, userId: String) { /* no-op */ }
    fun leaveRoom(roomId: String) { /* no-op */ }
    fun pinRoom(roomId: String) { /* no-op */ }
    fun muteRoom(roomId: String) { /* no-op */ }
    fun unpinRoom(roomId: String) { /* no-op */ }
    fun unmuteRoom(roomId: String) { /* no-op */ }
    fun archiveRoom(roomId: String) { /* no-op */ }
    fun unarchiveRoom(roomId: String) { /* no-op */ }
    fun renameGroup(roomId: String, newName: String) { /* no-op */ }
    fun transferAdmin(roomId: String, newAdminId: String) { /* no-op */ }
    fun markRoomAsRead(roomId: String) { /* no-op */ }

    fun sendMessage(content: String, roomId: String, userId: String, type: String) {
        socket.emit("send_message", content, roomId, userId, type)
    }
}

/**
 * Very small in-memory socket-like helper with simple on/emit.
 */
class SimpleSocket {
    private val handlers = mutableMapOf<String, MutableList<(Array<Any?>) -> Unit>>()

    fun on(event: String, handler: (Array<Any?>) -> Unit) {
        handlers.getOrPut(event) { mutableListOf() }.add(handler)
    }

    fun on(event: String, handler: () -> Unit) {
        val wrapped: (Array<Any?>) -> Unit = { handler() }
        handlers.getOrPut(event) { mutableListOf() }.add(wrapped)
    }

    fun emit(event: String, vararg args: Any?) {
        handlers[event]?.forEach { it(args as Array<Any?>) }
    }
}