package com.example.client.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.client.model.data.ChatRoom
import com.example.client.model.data.Message
import com.example.client.model.data.User
import com.example.client.model.repository.SocketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    val repository: SocketRepository,
    val currentUserId: String
) : ViewModel() {

    private val TAG = "ChatViewModel"

    // Danh sách tin nhắn hiển thị trên màn hình chat
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    // Quan sát danh sách các phòng chat từ Repository
    val rooms = repository.rooms

    /**
     * SỬA ĐỔI: Lọc danh sách bạn bè
     * Sử dụng toán tử lọc nghiêm ngặt để đảm bảo đồng bộ với Database.
     */
    val friends: StateFlow<List<User>> = repository.users
        .map { allUsers ->
            // Tìm thông tin của tôi trong danh sách trả về
            val myInfo = allUsers.find { it.id == currentUserId }
            // Lấy danh sách ID bạn bè từ myInfo
            val myFriendIds = myInfo?.friends ?: emptyList()

            Log.d(TAG, "My ID: $currentUserId")
            Log.d(TAG, "My Friends in DB: $myFriendIds")

            // Lọc: Phải có ID nằm trong danh sách bạn bè của tôi
            allUsers.filter { user ->
                user.id != currentUserId && myFriendIds.contains(user.id)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ID phòng đang chat hiện tại
    var activeRoomId: String = ""
        private set

    init {
        // Yêu cầu danh sách phòng ngay khi khởi tạo
        refreshRooms()

        // Lắng nghe tin nhắn mới để cập nhật UI
        viewModelScope.launch(Dispatchers.IO) {
            repository.messagesByRoom.collect { map ->
                if (activeRoomId.isNotBlank()) {
                    val roomMessages = map[activeRoomId] ?: emptyList()
                    _messages.value = roomMessages
                    Log.d(TAG, "Đã cập nhật UI cho phòng $activeRoomId: ${roomMessages.size} tin nhắn")
                }
            }
        }
    }

    // Thiết lập phòng chat khi người dùng nhấn vào một cuộc hội thoại
    fun setActiveRoom(roomId: String, roomName: String) {
        activeRoomId = roomId
        _messages.value = repository.messagesByRoom.value[roomId] ?: emptyList()
        repository.joinRoom(roomId)
        repository.syncMessages(roomId)
    }

    // Gửi tin nhắn văn bản
    fun sendMessage(content: String) {
        if (activeRoomId.isBlank()) return
        repository.sendMessage(
            content = content,
            roomId = activeRoomId,
            userId = currentUserId,
            type = "TEXT"
        )
    }

    // Gửi tin nhắn hình ảnh
    fun sendImage(context: Context, uri: Uri) {
        if (activeRoomId.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    repository.sendMessage(
                        content = base64String,
                        roomId = activeRoomId,
                        userId = currentUserId,
                        type = "IMAGE"
                    )
                    Log.d(TAG, "Gửi ảnh thành công")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi xử lý ảnh: ${e.message}")
            }
        }
    }

    // Đánh dấu đã đọc phòng chat
    fun markAsSeen(message: Message) {
        if (message.senderId != currentUserId) {
            repository.markRoomAsRead(message.roomId)
        }
    }

    // Yêu cầu server gửi lại danh sách phòng chat
    fun refreshRooms() {
        if (currentUserId.isNotBlank()) {
            repository.requestRooms(currentUserId)
        }
    }

    fun connect(token: String, userId: String) {
        repository.connect(token)
    }

    // Bắt đầu chat 1-1
    fun startPrivateChat(user: User): ChatRoom {
        val room = repository.ensurePrivateRoom(currentUserId, user)
        setActiveRoom(room.id, room.name)
        return room
    }

    // Làm mới dữ liệu
    fun refreshData() {
        refreshRooms()
        // Kích hoạt server gửi lại danh sách người dùng và trạng thái bạn bè
        repository.requestOnlineUsers()
    }

    fun joinExistingRoom(room: ChatRoom) {
        setActiveRoom(room.id, room.name)
    }

    // Tạo nhóm chat
    fun createGroup(name: String, memberIds: List<String>): ChatRoom {
        val room = repository.createGroup(name, memberIds)
        refreshRooms()
        return room
    }

    fun markRoomAsRead(roomId: String) {
        repository.markRoomAsRead(roomId)
    }

    fun onUserInputChanged(text: String) {
        Log.d(TAG, "User input changed")
    }

    fun disconnect() {
        repository.disconnect()
    }
}