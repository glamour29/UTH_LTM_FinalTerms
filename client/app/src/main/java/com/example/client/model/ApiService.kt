package com.example.client.model

import com.example.client.model.data.ChatRoom
import com.example.client.model.data.User
import retrofit2.http.*

interface ApiService {
    @GET("api/rooms")
    suspend fun getRooms(@Header("Authorization") token: String): List<ChatRoom>

    @POST("api/rooms/group")
    suspend fun createGroup(
        @Header("Authorization") token: String,
        @Body request: CreateGroupRequest
    ): ChatRoom

    @GET("api/users")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("search") query: String
    ): List<User>
}

data class CreateGroupRequest(
    val name: String,
    val members: List<String>
)
