package com.example.myapplication.data.model

data class Group(
    val id: String,
    val name: String,
    val avatar: String? = null,
    val description: String? = null,
    val adminId: String,
    val members: List<GroupMember> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class GroupMember(
    val userId: String,
    val role: GroupRole,
    val joinedAt: Long = System.currentTimeMillis()
)

enum class GroupRole {
    ADMIN, MEMBER
}
