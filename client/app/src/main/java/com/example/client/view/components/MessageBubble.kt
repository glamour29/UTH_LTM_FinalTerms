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
import com.example.client.utils.decodeBase64ToBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun MessageBubble(message: Message, isMe: Boolean,onSeen: () -> Unit = {}) {
    LaunchedEffect(Unit) {
        if (!isMe) onSeen()
    }
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isMe) 18.dp else 4.dp,
        bottomEnd = if (isMe) 4.dp else 18.dp
    )


    val isImage = message.type.equals("image", ignoreCase = true)
            || message.content.startsWith("data:image")

    val imageBitmap = remember(message.content) {
        if (isImage) decodeBase64ToBitmap(message.content) else null
    }
    val myMessageColor = Color(0xFFFFC107)

    // -----------------------

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isImage) Color.Transparent // Nếu là ảnh -> Trong suốt
                    else if (isMe) myMessageColor // Nếu là text mình gửi -> Xanh
                    else Color(0xFFE4E6EB), // Nếu là text bạn gửi -> Xám
                    shape = bubbleShape
                )
                // SỬA Ở ĐÂY: Dùng isImage để bỏ padding nếu là ảnh
                .padding(if (isImage) 0.dp else 12.dp)
        ) {
            // SỬA Ở ĐÂY: Dùng biến isImage cho gọn
            if (isImage) {
                // --- HIỂN THỊ ẢNH TỪ BITMAP ---
                if (imageBitmap != null) {
                    AsyncImage(
                        model = imageBitmap,
                        contentDescription = "Gửi ảnh",
                        modifier = Modifier
                            .widthIn(max = 250.dp)
                            .heightIn(max = 350.dp)
                            .clip(bubbleShape), // Bo góc ảnh theo hình tin nhắn
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Lỗi tải ảnh", color = Color.Red, modifier = Modifier.padding(8.dp))
                }

            } else {
                // --- HIỂN THỊ TEXT ---
                Text(
                    text = message.content,
                    color = if (isMe) Color.White else Color.Black,
                    fontSize = 16.sp
                )
            }
        }

        if (isMe && message.status == "seen") {
            Text(
                text = "Đã xem",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(end = 4.dp, top = 2.dp)
            )
        }
    }
}