package com.example.client.model

import com.example.client.model.data.ChatRoom
import com.example.client.model.data.User
import retrofit2.http.*
import retrofit2.Response

interface ApiService {
    // --- PHÒNG CHAT & NHÓM ---
    @GET("api/rooms")
    suspend fun getRooms(@Header("Authorization") token: String): List<ChatRoom>

    @POST("api/rooms/group")
    suspend fun createGroup(
        @Header("Authorization") token: String,
        @Body request: CreateGroupRequest
    ): ChatRoom

    @POST("api/rooms/{roomId}/members")
    suspend fun addMember(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String,
        @Body request: MemberRequest
    ): ChatRoom

    @DELETE("api/rooms/{roomId}/members/{userId}")
    suspend fun removeMember(
        @Header("Authorization") token: String,
        @Path("roomId") roomId: String,
        @Path("userId") userId: String
    ): ChatRoom

    // --- NGƯỜI DÙNG & KẾT BẠN ---


    @GET("api/users/friends")
    suspend fun getFriends(@Header("Authorization") token: String): List<User>

    @GET("api/users/friends/pending")
    suspend fun getPendingRequests(@Header("Authorization") token: String): List<User>

    @POST("api/users/friends/request")
    suspend fun sendFriendRequest(
        @Header("Authorization") token: String,
        @Body request: FriendRequest
    ): GenericResponse


    @POST("api/users/friends/accept")
    suspend fun acceptFriendRequest(
        @Header("Authorization") token: String,
        @Body request: FriendRequest
    ): GenericResponse
    // Trong ApiService.kt
    // Sửa lại cho đúng URL của server (thêm "api/" nếu cần)
    @GET("api/users/search")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): Response<List<User>>

}
data class CreateGroupRequest(val name: String, val members: List<String>)
data class MemberRequest(val userId: String)
data class FriendRequest(val userId: String)
data class GenericResponse(val success: Boolean, val message: String)
