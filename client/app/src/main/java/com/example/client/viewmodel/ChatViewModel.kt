package com.example.client.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.lifecycle.ViewModel
import com.example.client.model.data.Message
import com.example.client.model.repository.SocketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
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
            repository.sendMessage(content, currentRoomId, currentUserId, "TEXT")
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
    fun sendImage(context: Context, uri: Uri) {
        val base64Image = uriToBase64(context, uri)
        if (base64Image != null) {
            // Gửi với type là IMAGE. Chuỗi base64 cần có prefix để hiển thị được
            val content = "data:image/jpeg;base64,$base64Image"
            repository.sendMessage(content, currentRoomId, currentUserId, "IMAGE")
        }
    }

    // Hàm phụ trợ chuyển Uri sang Base64
    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            // Nén ảnh xuống định dạng JPEG, chất lượng 50% để giảm dung lượng
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}