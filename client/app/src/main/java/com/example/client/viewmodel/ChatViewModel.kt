package com.example.client.viewmodel

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.client.model.data.Message
import com.example.client.model.repository.SocketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
class ChatViewModel : ViewModel() {
    private val repository = SocketRepository()

    val messages: StateFlow<List<Message>> = repository.messages

    // Giả lập thông tin User hiện tại (Sau này lấy từ Login của Kiên)
    val currentUserId = "user_${UUID.randomUUID().toString().substring(0, 5)}"
    val currentRoomId = "room_abc"

    private val _typingUser = MutableStateFlow<String?>(null)
    val typingUser: StateFlow<String?> = _typingUser

    // Biến hỗ trợ debounce (tránh gửi signal liên tục)
    private var typingHandler: Handler = Handler(Looper.getMainLooper())
    private val stopTypingRunnable = Runnable {
        repository.sendStopTyping(currentRoomId)
        _typingUser.value = null
    }

    init {
        repository.connect()
        joinRoom(currentRoomId)
        repository.socket.on("user_typing") { args ->
            val userId = args[0] as String
            if (userId != currentUserId) {
                _typingUser.value = userId
            }
        }

        repository.socket.on("user_stopped_typing") {
            _typingUser.value = null
        }

        // Lắng nghe sự kiện tin nhắn đã được xem
        repository.socket.on("message_seen_updated") { args ->
            // Logic cập nhật trạng thái tin nhắn trong list messages thành "seen"
            // (Bạn cần viết hàm update list message tại đây)
        }
    }

    fun sendMessage(content: String) {
        if (content.isNotBlank()) {
            repository.sendMessage(content, currentRoomId, currentUserId)
        }
    }

    fun joinRoom(roomId: String) {
        repository.joinRoom(roomId)
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
    fun onUserInputChanged(text: String) {
        if (text.isNotBlank()) {
            // Gửi sự kiện đang gõ
            repository.socket.emit("typing", currentRoomId)

            // Hủy lệnh dừng cũ và đặt lệnh dừng mới sau 2 giây
            typingHandler.removeCallbacks(stopTypingRunnable)
            typingHandler.postDelayed(stopTypingRunnable, 2000)
        }
    }

    // Hàm báo đã xem tin nhắn (gọi khi MessageBubble hiển thị)
    fun markAsSeen(message: Message) {
        if (message.senderId != currentUserId && message.status != "seen") {
            val data = JSONObject()
            data.put("roomId", currentRoomId)
            data.put("messageId", message.id)
            repository.socket.emit("mark_seen", data)
        }
    }
}