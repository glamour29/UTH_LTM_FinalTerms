package com.example.client.model.repository

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
            // Thay đổi địa chỉ IP này thành IP máy chủ của bạn (giữ nguyên port server nodejs)
            // Lưu ý: Dùng máy thật thì dùng IP LAN (ví dụ 192.168.1.x), máy ảo thì 10.0.2.2
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

    fun sendMessage(content: String, roomId: String, senderId: String) {
        val jsonObject = JSONObject()
        jsonObject.put("roomId", roomId)
        jsonObject.put("content", content)
        jsonObject.put("senderId", senderId)

        mSocket?.emit("send_message", jsonObject)
    }


    fun sendStopTyping(roomId: String) {
        mSocket?.emit("stop_typing", roomId)
    }

    // Hàm phụ trợ để chuyển JSON thành Object Message
    private fun parseMessage(json: JSONObject): Message {
        return Message(
            // Nếu không có id, tự tạo ID ngẫu nhiên để không lỗi UI
            id = json.optString("id", UUID.randomUUID().toString()),

            // Nếu không có nội dung, trả về chuỗi rỗng
            content = json.optString("content", ""),

            // Nếu không có senderId, trả về ẩn danh
            senderId = json.optString("senderId", "anonymous"),

            // SỬA LỖI TIMESTAMP TẠI ĐÂY:
            // Dùng optLong: Nếu server gửi null hoặc String sai định dạng,
            // nó sẽ lấy System.currentTimeMillis() (giờ hiện tại trên máy)
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),

            status = json.optString("status", "sent")
        )
    }
}