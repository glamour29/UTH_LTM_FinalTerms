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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    // ID phòng đang chat hiện tại
    var activeRoomId: String = ""
        private set

    init {
        // 1. Yêu cầu danh sách phòng ngay khi ViewModel được khởi tạo
        refreshRooms()

        // 2. LUỒNG TỰ ĐỘNG CẬP NHẬT: Luôn lắng nghe tin nhắn mới từ Repository
        viewModelScope.launch(Dispatchers.IO) {
            repository.messagesByRoom.collect { map ->
                if (activeRoomId.isNotBlank()) {
                    // Cập nhật danh sách tin nhắn ngay khi Map trong Repository thay đổi
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

        // Lấy nhanh tin nhắn từ cache hiển thị lên trước
        _messages.value = repository.messagesByRoom.value[roomId] ?: emptyList()

        // Tham gia phòng và đồng bộ tin nhắn mới nhất từ Server
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

    // Gửi tin nhắn hình ảnh (Chuyển đổi sang Base64)
    fun sendImage(context: Context, uri: Uri) {
        if (activeRoomId.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    // Sử dụng Base64.NO_WRAP để tránh lỗi định dạng JSON khi gửi chuỗi dài
                    val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)

                    repository.sendMessage(
                        content = base64String,
                        roomId = activeRoomId,
                        userId = currentUserId,
                        type = "IMAGE"
                    )
                    Log.d(TAG, "Gửi ảnh thành công qua Socket")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi xử lý ảnh: ${e.message}")
            }
        }
    }

    // Đánh dấu đã đọc phòng chat
    fun markAsSeen(message: Message) {
        if (message.senderId != currentUserId) {
            repository.markRoomAsRead(message.roomId)
        }
    }

    // Hàm yêu cầu Server gửi lại danh sách phòng chat
    fun refreshRooms() {
        if (currentUserId.isNotBlank()) {
            repository.requestRooms(currentUserId)
            Log.d(TAG, "Đã gọi requestRooms cho User: $currentUserId")
        }
    }
    // Thêm vào ChatViewModel.kt
    fun connect(token: String, userId: String) {
        repository.connect(token)
        // Bạn có thể lưu userId vào biến local nếu cần
    }
    // Thêm vào ChatViewModel.kt

    // Dùng cho trường hợp nhấn vào một User để bắt đầu chat 1-1
    fun startPrivateChat(user: com.example.client.model.data.User): com.example.client.model.data.ChatRoom {
        val room = repository.ensurePrivateRoom(currentUserId, user)
        setActiveRoom(room.id, room.name)
        return room
    }
    val friends: StateFlow<List<User>> = repository.users // Hoặc repository.friends tùy tên bạn đặt trong Repo

    // 2. Thêm hàm refreshData (để App gọi khi vào màn hình mới)
    fun refreshData() {
        refreshRooms()
        // Nếu có hàm lấy danh sách bạn bè riêng trong repo, hãy gọi ở đây
        repository.requestOnlineUsers()
    }
    // Thêm vào ChatViewModel.kt để fix lỗi đỏ trong UsersScreenImproved
    fun joinExistingRoom(room: ChatRoom) {
        setActiveRoom(room.id, room.name)
    }

    // Hàm tạo nhóm
    fun createGroup(name: String, memberIds: List<String>): com.example.client.model.data.ChatRoom {
        val room = repository.createGroup(name, memberIds)
        refreshRooms() // Load lại danh sách phòng để thấy nhóm mới
        return room
    }
    fun markRoomAsRead(roomId: String) {
        repository.markRoomAsRead(roomId)
    }
    fun onUserInputChanged(text: String) {
        // Hiện tại không làm gì vì đã bỏ chức năng typing theo yêu cầu của bạn
        // Nhưng giữ hàm này để các Screen gọi không bị báo lỗi đỏ
        Log.d(TAG, "User input changed: ${text.length} chars")
    }

    fun disconnect() {
        repository.disconnect()
    }
}