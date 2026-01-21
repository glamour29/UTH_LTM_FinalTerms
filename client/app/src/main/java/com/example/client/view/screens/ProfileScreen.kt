package com.example.client.view.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import com.example.client.R // Nếu có ảnh avatar mặc định trong drawable

@Composable
fun ProfileScreen(
    onLogout: () -> Unit // Callback để báo cho MainActivity biết là đã đăng xuất
) {
    val context = LocalContext.current

    // 1. Lấy thông tin user đã lưu trong máy
    val sharedPref = context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE)
    val username = sharedPref.getString("USERNAME", "Unknown User") ?: "User"
    // val fullName = sharedPref.getString("FULL_NAME", "Người dùng") // Nếu lúc login có lưu tên thật

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 2. Avatar (Tạm thời dùng hình tròn màu xám hoặc icon)
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(text = username.take(1).uppercase(), fontSize = 40.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Hiển thị tên
        Text(text = "Xin chào,", fontSize = 18.sp, color = Color.Gray)
        Text(text = username, fontSize = 28.sp, style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(40.dp))

        // 4. Các nút chức năng (Placeholder cho vui)
        Button(
            onClick = { /* Chưa làm gì cả */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Chỉnh sửa thông tin (Coming Soon)")
        }

        Spacer(modifier = Modifier.weight(1f)) // Đẩy nút logout xuống đáy

        // 5. NÚT ĐĂNG XUẤT (QUAN TRỌNG)
        Button(
            onClick = {
                // A. Ngắt kết nối Socket ngay lập tức
                try {
                    com.example.client.api.SocketHandler.closeConnection()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // B. Xóa Token trong máy
                with(sharedPref.edit()) {
                    clear()
                    apply()
                }

                // C. Chuyển màn hình
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("ĐĂNG XUẤT", color = Color.White)
        }
    }
}
