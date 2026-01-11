package com.example.myapplication.data.model

data class Message(
    val id: String,
    val senderId: String,
    val receiverId: String? = null,
    val groupId: String? = null,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENDING,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false
)

enum class MessageType {
    TEXT, IMAGE, FILE
}

enum class MessageStatus {
    SENDING, SENT, DELIVERED, SEEN, FAILED
}
