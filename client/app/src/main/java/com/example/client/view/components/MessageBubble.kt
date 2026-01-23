package com.example.client.view.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.client.model.data.Message
import com.example.client.utils.decodeBase64ToBitmap
import com.example.client.view.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(
    message: Message,
    currentUserId: String, // Thay đổi quan trọng: Truyền ID của user hiện tại vào đây
    onSeen: () -> Unit = {}
) {
    // Xác định trực tiếp dựa trên senderId và currentUserId
    // Việc này giúp tránh lỗi khi Re-login bằng acc khác
    val isMe = remember(message.senderId, currentUserId) {
        message.senderId == currentUserId
    }

    LaunchedEffect(isMe) {
        if (!isMe) onSeen()
    }

    val isImage = remember(message.content) {
        message.type.equals("image", ignoreCase = true) ||
                message.content.startsWith("data:image")
    }

    val isVoice = remember(message.type, message.content) {
        message.type.equals("voice", ignoreCase = true) ||
                message.content.startsWith("voice:")
    }

    val displayText = if (isVoice) "Voice message" else message.content

    val imageBitmap = remember(message.content, isImage) {
        if (isImage) decodeBase64ToBitmap(message.content) else null
    }

    val timeText = remember(message.timestamp) {
        if (message.timestamp > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
        } else {
            ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        // Cố định: Tin nhắn của mình luôn sang phải (End), người khác bên trái (Start)
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.widthIn(max = 320.dp), // Tăng độ rộng tối đa một chút
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            // Hiển thị Avatar nếu KHÔNG PHẢI là mình
            if (!isMe) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = TealLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = message.senderId.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
            }

            // Nội dung tin nhắn
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                ),
                color = when {
                    isImage -> Color.Transparent
                    isMe -> TealPrimary // Màu thương hiệu cho mình
                    else -> Color(0xFFE9E9EB) // Màu xám nhạt trung tính cho người khác
                },
                shadowElevation = if (isImage) 0.dp else 0.5.dp
            ) {
                when {
                    isImage && imageBitmap != null -> {
                        AsyncImage(
                            model = imageBitmap,
                            contentDescription = "Image message",
                            modifier = Modifier
                                .widthIn(max = 240.dp)
                                .heightIn(max = 320.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    else -> {
                        Text(
                            text = displayText,
                            color = if (isMe) Color.White else Color.Black,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Thời gian gửi tin nhắn
        if (timeText.isNotEmpty()) {
            Text(
                text = timeText,
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(
                    top = 2.dp,
                    start = if (isMe) 0.dp else 40.dp, // Tránh đè lên Avatar
                    end = if (isMe) 4.dp else 0.dp
                )
            )
        }
    }
}