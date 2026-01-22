// Message.kt
package com.example.client.model.data
import org.json.JSONObject

data class Message(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val content: String = "",
    val type: String = "TEXT",
    val createdAt: String = "", // Server đã gửi string "HH:mm" về đây rồi
    val timestamp: Long = 0L,
    val senderName: String = "",
    val status: String = "sent"
) {
    companion object {
        fun fromJson(json: JSONObject): Message {
            return Message(
                id = json.optString("id"),
                roomId = json.optString("roomId"),
                senderId = json.optString("senderId"),
                content = json.optString("content"),
                type = json.optString("type", "TEXT"),

                // Lấy createdAt từ server, nếu không có thì để rỗng
                createdAt = json.optString("createdAt", ""),

                // Lấy timestamp, quan trọng để sắp xếp
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),

                status = json.optString("status", "sent")
            )
        }
    }
}