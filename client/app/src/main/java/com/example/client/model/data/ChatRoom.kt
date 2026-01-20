package com.example.client.model.data

data class ChatRoom(
    val id: String = "",
    val name: String = "",
    val isGroup: Boolean = false,
    val memberIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastUpdated: Long = 0L,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false
)