package com.example.client.model.data

import com.google.gson.annotations.SerializedName

data class ChatRoom(
    // QUAN TRỌNG: Phải có dòng này để map "_id" từ MongoDB vào biến "id" của Kotlin
    @SerializedName("_id")
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