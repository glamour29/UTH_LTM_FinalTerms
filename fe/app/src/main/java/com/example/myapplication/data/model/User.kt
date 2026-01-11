package com.example.myapplication.data.model

data class User(
    val id: String,
    val username: String,
    val email: String? = null,
    val avatar: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null
)
