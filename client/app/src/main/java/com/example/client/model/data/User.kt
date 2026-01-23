package com.example.client.model.data

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id")
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String = "",
    val phoneNumber: String = "",
    val friends: List<String> = emptyList(),
    val isOnline: Boolean = false
)
