package com.example.myapplication.data.model

data class Chat(
    val id: String,
    val type: ChatType,
    val name: String? = null,
    val avatar: String? = null,
    val participants: List<String> = emptyList(),
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isMuted: Boolean = false
)

enum class ChatType {
    PRIVATE, GROUP
}
