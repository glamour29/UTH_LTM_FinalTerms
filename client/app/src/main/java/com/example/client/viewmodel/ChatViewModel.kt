package com.example.client.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.client.model.*
import com.example.client.model.data.ChatRoom
import com.example.client.model.data.Message
import com.example.client.model.data.User
import com.example.client.model.repository.SocketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val repository: SocketRepository = SocketRepository()
) : ViewModel() {

    private val apiService = RetrofitClient.instance

    val users: StateFlow<List<User>> = repository.users
    
    private val _friends = MutableStateFlow<List<User>>(emptyList())
    val friends: StateFlow<List<User>> = _friends

    private val _pendingRequests = MutableStateFlow<List<User>>(emptyList())
    val pendingRequests: StateFlow<List<User>> = _pendingRequests

    private val _rooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val rooms: StateFlow<List<ChatRoom>> = _rooms

    var currentUserId: String = ""
    private var authToken: String = ""

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private var activeRoomId: String = ""
    var currentRoomId: String = ""

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.messagesByRoom.collectLatest { map ->
                if (activeRoomId.isNotBlank()) {
                    val list = map[activeRoomId] ?: emptyList()
                    _messages.value = list
                }
            }
        }
        
        viewModelScope.launch {
            repository.rooms.collect { socketRooms ->
                if (socketRooms.isNotEmpty()) {
                    _rooms.value = socketRooms
                }
            }
        }
    }

    fun connect(token: String, userId: String) {
        authToken = token
        currentUserId = userId
        repository.connect(token)
        refreshData()
    }

    fun refreshData() {
        fetchRooms()
        fetchFriends()
        fetchPendingRequests()
    }

    // --- PHẦN API CHAT & NHÓM ---
    fun fetchRooms() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getRooms("Bearer $authToken")
                _rooms.value = response
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun createGroup(name: String, memberIds: List<String>): ChatRoom {
        val tempRoom = ChatRoom(id = "temp_${UUID.randomUUID()}", name = name, isGroup = true, memberIds = memberIds)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.createGroup("Bearer $authToken", CreateGroupRequest(name, memberIds))
                fetchRooms()
            } catch (e: Exception) { e.printStackTrace() }
        }
        return tempRoom
    }

    fun addMember(roomId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.addMember("Bearer $authToken", roomId, MemberRequest(userId))
                repository.addMember(roomId, userId)
                fetchRooms()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun kickMember(roomId: String, userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.removeMember("Bearer $authToken", roomId, userId)
                repository.kickMember(roomId, userId)
                fetchRooms()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- PHẦN KẾT BẠN ---
    fun fetchFriends() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val friendsList = apiService.getFriends("Bearer $authToken")
                _friends.value = friendsList
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun fetchPendingRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requests = apiService.getPendingRequests("Bearer $authToken")
                _pendingRequests.value = requests
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun sendFriendRequest(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.sendFriendRequest("Bearer $authToken", FriendRequest(userId))
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun acceptFriendRequest(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.acceptFriendRequest("Bearer $authToken", FriendRequest(userId))
                refreshData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun searchUsers(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiService.searchUsers("Bearer $authToken", query)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- SOCKET & MESSAGES ---
    fun setActiveRoom(roomId: String, roomName: String) {
        activeRoomId = roomId
        repository.syncMessages(roomId)
    }

    fun markRoomAsRead(roomId: String) = repository.markRoomAsRead(roomId)

    fun onUserInputChanged(text: String) {
        repository.sendStopTyping(activeRoomId)
    }

    fun markAsSeen(message: Message) {
        try {
            repository.sendMessage(message.content, message.roomId, message.senderId, message.type)
        } catch (_: Exception) { }
    }

    fun sendImage(context: Context, uri: Uri) {
        val placeholder = "image:$uri"
        sendMessage(placeholder)
    }

    fun sendMessage(content: String) {
        if (activeRoomId.isBlank() || currentUserId.isBlank()) return
        val type = if (content.startsWith("data:image") || content.startsWith("image:")) "image" else "text"

        val tempId = "local_${UUID.randomUUID()}"
        val tempMessage = Message(
            id = tempId, roomId = activeRoomId, senderId = currentUserId,
            content = content, type = type, createdAt = "", timestamp = System.currentTimeMillis(), status = "sending"
        )

        val current = _messages.value.toMutableList()
        current.add(tempMessage)
        _messages.value = current

        viewModelScope.launch(Dispatchers.IO) {
            repository.sendMessage(content, activeRoomId, currentUserId, type)
        }
    }

    fun disconnect() = repository.disconnect()
    fun leaveRoom(roomId: String) = repository.leaveRoom(roomId)
    fun pinRoom(roomId: String) = repository.pinRoom(roomId)
    fun unpinRoom(roomId: String) = repository.unpinRoom(roomId)
    fun muteRoom(roomId: String) = repository.muteRoom(roomId)
    fun unmuteRoom(roomId: String) = repository.unmuteRoom(roomId)
    fun archiveRoom(roomId: String) = repository.archiveRoom(roomId)
    fun unarchiveRoom(roomId: String) = repository.unarchiveRoom(roomId)
    fun joinExistingRoom(room: ChatRoom) = repository.joinRoom(room.id)
    fun startPrivateChat(user: User): ChatRoom = repository.ensurePrivateRoom(currentUserId, user)
    fun renameGroup(roomId: String, newName: String) = repository.renameGroup(roomId, newName)
    fun transferAdmin(roomId: String, newAdminId: String) = repository.transferAdmin(roomId, newAdminId)
}
