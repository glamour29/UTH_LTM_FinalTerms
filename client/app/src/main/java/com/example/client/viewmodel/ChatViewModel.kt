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
    initialUserId: String // ID ban đầu khi khởi tạo
) : ViewModel() {

    private val TAG = "ChatViewModel"

    // Sử dụng StateFlow để quản lý ID người dùng hiện tại, cho phép cập nhật khi đổi acc
    private val _currentUserId = MutableStateFlow(initialUserId)
    val currentUserIdState: StateFlow<String> = _currentUserId.asStateFlow()

    // Getter tiện lợi để lấy ID hiện tại (vẫn giữ tên biến cũ để không hỏng code cũ)
    val currentUserId: String get() = _currentUserId.value

    // Danh sách tin nhắn hiển thị trên màn hình chat
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    // Quan sát danh sách các phòng chat từ Repository
    val rooms = repository.rooms

    /**
     * Lọc danh sách bạn bè dựa trên currentUserIdState
     * Khi ID thay đổi, danh sách bạn bè sẽ tự động tính toán lại
     */
    val friends: StateFlow<List<User>> = combine(repository.users, _currentUserId) { allUsers, userId ->
        val myInfo = allUsers.find { it.id == userId }
        val myFriendIds = myInfo?.friends ?: emptyList()

        Log.d(TAG, "Cập nhật danh sách bạn bè cho User: $userId")

        allUsers.filter { user ->
            user.id != userId && myFriendIds.contains(user.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
                }
            }
        }
    }

    /**
     * HÀM QUAN TRỌNG: Cập nhật thông tin khi đăng nhập tài khoản mới
     */
    fun updateCurrentUser(newUserId: String) {
        Log.d(TAG, "Cập nhật User ID mới: $newUserId")
        _currentUserId.value = newUserId
        refreshRooms()
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
        if (activeRoomId.isBlank() || currentUserId.isBlank()) return
        repository.sendMessage(
            content = content,
            roomId = activeRoomId,
            userId = currentUserId,
            type = "TEXT"
        )
    }

    fun createNewGroup(name: String, selectedMemberIds: List<String>) {
        val finalMembers = selectedMemberIds.toMutableList().apply {
            if (!contains(currentUserId)) add(currentUserId)
        }

        repository.createChatGroup(name, finalMembers)

        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            refreshRooms()
        }
    }

    // Gửi tin nhắn hình ảnh
    fun sendImage(context: Context, uri: Uri) {
        if (activeRoomId.isBlank() || currentUserId.isBlank()) return

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
        updateCurrentUser(userId) // Cập nhật ID khi kết nối
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
        repository.requestOnlineUsers()
    }

    fun joinExistingRoom(room: ChatRoom) {
        setActiveRoom(room.id, room.name)
    }

    fun createGroup(name: String, memberIds: List<String>): ChatRoom {
        val room = repository.createGroup(name, memberIds)
        refreshRooms()
        return room
    }

    fun markRoomAsRead(roomId: String) {
        repository.markRoomAsRead(roomId)
    }

    fun onUserInputChanged(text: String) {
        // Logic xử lý khi người dùng đang nhập (typing...) nếu cần
    }

    fun disconnect() {
        repository.disconnect()
        _currentUserId.value = "" // Xóa ID khi ngắt kết nối
    }
}