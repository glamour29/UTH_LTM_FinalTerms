// File: `app/src/main/java/com/example/client/viewmodel/ChatViewModel.kt`
package com.example.client.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.client.model.data.ChatRoom
import com.example.client.model.data.Message
import com.example.client.model.data.User
import com.example.client.model.repository.SocketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel shim that provides the methods and properties the UI expects.
 * This is intentionally simple and delegates to the SocketRepository stub.
 */
class ChatViewModel(
    private val repository: SocketRepository = SocketRepository()
) : ViewModel() {

    // Public flows from repository
    val users: StateFlow<List<User>> = repository.users
    val rooms: StateFlow<List<ChatRoom>> = repository.rooms

    // Current logged-in user id (stub)
    var currentUserId: String = "" // set by app when user signs in

    // Active room and messages for that room (UI expects `messages` as a list flow)
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var activeRoomId: String = ""

    // Set active room and update messages flow
    fun setActiveRoom(roomId: String, roomName: String) {
        activeRoomId = roomId
        // Ask repository to sync messages (stub is no-op)
        repository.syncMessages(roomId)
        // Pull any messages already present in repo's backing map (stub may be empty)
        val map = repository.messagesByRoom
        // repository.messagesByRoom is a StateFlow; read its current value if available
        try {
            val currentMap = (map as? StateFlow<Map<String, List<Message>>>)?.value
            _messages.value = currentMap?.get(roomId) ?: emptyList()
        } catch (_: Exception) {
            _messages.value = emptyList()
        }
    }

    fun markRoomAsRead(roomId: String) {
        repository.markRoomAsRead(roomId)
    }

    fun sendImage(context: Context, uri: Uri) {
        // Convert to a placeholder string or call repository to upload/send
        val placeholder = "image:$uri"
        sendMessage(placeholder)
    }

    fun markAsSeen(message: Message) {
        // no-op in stub; repository could be extended to handle this
    }

    fun onUserInputChanged(text: String) {
        // Could emit typing events via repository.socket - stub no-op
        repository.sendStopTyping(activeRoomId)
    }

    // UI calls sendMessage with a single content param; use activeRoomId and currentUserId
    fun sendMessage(content: String) {
        if (activeRoomId.isBlank() || currentUserId.isBlank()) return
        val type = if (content.startsWith("data:image") || content.startsWith("image:")) "image" else "text"
        repository.sendMessage(content, activeRoomId, currentUserId, type)
    }

    // Room and member management helpers expected by UI
    fun joinExistingRoom(room: ChatRoom) {
        repository.joinRoom(room.id)
    }

    fun leaveRoom(roomId: String) = repository.leaveRoom(roomId)
    fun pinRoom(roomId: String) = repository.pinRoom(roomId)
    fun unpinRoom(roomId: String) = repository.unpinRoom(roomId)
    fun muteRoom(roomId: String) = repository.muteRoom(roomId)
    fun unmuteRoom(roomId: String) = repository.unmuteRoom(roomId)
    fun archiveRoom(roomId: String) = repository.archiveRoom(roomId)
    fun unarchiveRoom(roomId: String) = repository.unarchiveRoom(roomId)

    // Start or get a private room with a user
    fun startPrivateChat(user: User): ChatRoom {
        return repository.ensurePrivateRoom(currentUserId, user)
    }

    // Create group (UI calls without currentUserId)
    fun createGroup(name: String, memberIds: List<String>): ChatRoom {
        return repository.createGroup(name, memberIds, currentUserId)
    }

    // Fallbacks for any other repository APIs
}