package com.example.client.model.data

data class Message(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val content: String = "",
    val type: String = "TEXT", // TEXT, IMAGE, SYSTEM
    val createdAt: String = "",
    val timestamp: Long,
    var status: String = "sent"
)