package com.example.client.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.model.data.Message
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done

@Composable
fun MessageBubble(message: Message, isMe: Boolean,onSeen: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        if (!isMe) onSeen()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp), // Thêm padding dọc cho thoáng
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    // Màu sắc theo yêu cầu: Xanh da trời (Mình) - Xám trắng (Họ)
                    color = if (isMe) Color(0xFF0084FF) else Color(0xFFE4E6EB),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        // Tạo hiệu ứng đuôi tin nhắn
                        bottomStart = if (isMe) 18.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 18.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.content,
                // Màu chữ: Trắng (Mình) - Đen (Họ)
                color = if (isMe) Color.White else Color.Black,
                fontSize = 16.sp
            )
        }
        if (isMe && message.status == "seen") {
            Text(
                text = "Đã xem",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(end = 4.dp)
            )
        }
    }
}