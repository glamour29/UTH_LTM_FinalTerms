package com.example.client.models

// 1. Dữ liệu gửi lên khi Đăng nhập
data class LoginRequest(
    val username: String,
    val password: String
)

// 2. Dữ liệu gửi lên khi Đăng ký
data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String
)

// 3. Dữ liệu Server trả về (QUAN TRỌNG: Chứa Token)
data class LoginResponse(
    val success: Boolean?,
    val message: String?,
    val token: String?,     // 👈 Đây là vé thông hành
    val userId: String?,    // 👈 ID của người dùng
    val username: String?
)