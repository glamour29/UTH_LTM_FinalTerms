package com.example.client.model.repository

import android.R.attr.type
import android.util.Log
import com.example.client.model.data.Message
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.UUID

class SocketRepository {
    // Biến private chứa socket thực sự
    private var mSocket: Socket? = null

    val socket: Socket
        get() = mSocket ?: throw IllegalStateException("Socket chưa được khởi tạo!")

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    init {
        try {
            
            mSocket = IO.socket("http://10.0.2.2:3000")
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }
    }

    fun connect() {
        mSocket?.connect()

        // Lắng nghe tin nhắn mới từ server
        mSocket?.on("receive_message") { args ->
            if (args.isNotEmpty()) {
                val data = args[0] as JSONObject
                val message = parseMessage(data)
                // Cập nhật vào list hiện tại
                val currentList = _messages.value.toMutableList()
                currentList.add(message)
                _messages.value = currentList
            }
        }
    }

    fun disconnect() {
        mSocket?.disconnect()
        mSocket?.off()
    }

    fun joinRoom(roomId: String) {
        mSocket?.emit("join_room", roomId)
    }

    fun sendMessage(content: String, roomId: String, senderId: String, type: String = "TEXT") {
        val jsonObject = JSONObject()
        jsonObject.put("roomId", roomId)
        jsonObject.put("senderId", senderId)

        if (type == "IMAGE") {
            // === TRƯỜNG HỢP 1: GỬI ẢNH ===
            // Server yêu cầu key là 'imageBase64' và sự kiện 'send_image'
            jsonObject.put("imageBase64", content)

            Log.d("SocketRepo", "Đang gửi ẢNH...")
            mSocket?.emit("send_image", jsonObject)
        } else {

            jsonObject.put("content", content)
            jsonObject.put("type", "text") // Gửi kèm cho chắc chắn

            Log.d("SocketRepo", "Đang gửi TEXT: $content")
            mSocket?.emit("send_message", jsonObject)
        }
    }

    fun sendStopTyping(roomId: String) {
        mSocket?.emit("stop_typing", roomId)
    }

    private fun parseMessage(json: JSONObject): Message {
        // Log để kiểm tra server trả về type gì
        val typeFromServer = json.optString("type", "TEXT")
        Log.d("SocketRepository", "Nhận tin nhắn loại: $typeFromServer")

        return Message(
            id = json.optString("id", UUID.randomUUID().toString()),
            content = json.optString("content", ""),
            senderId = json.optString("senderId", "anonymous"),

            // Lấy type server trả về (quan trọng để MessageBubble hiển thị đúng)
            type = typeFromServer,

            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            status = json.optString("status", "sent")
        )
    }
    fun updateMessageStatus(messageId: String, newStatus: String) {
        // Lấy danh sách hiện tại
        val currentList = _messages.value.toMutableList()

        // Tìm vị trí tin nhắn
        val index = currentList.indexOfFirst { it.id == messageId }

        if (index != -1) {
            // Cập nhật trạng thái
            val oldMessage = currentList[index]
            // Lưu ý: Message phải là data class để dùng .copy()
            val updatedMessage = oldMessage.copy(status = newStatus)

            currentList[index] = updatedMessage

            // Đẩy dữ liệu mới vào luồng (StateFlow)
            _messages.value = currentList
        }
    }
    fun updateMessageList(newList: List<Message>) {

        _messages.value = newList
    }
}