package com.example.client.view.components

import androidx.compose.foundation.background
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
    isMe: Boolean,
    onSeen: () -> Unit = {}
) {
    LaunchedEffect(Unit) {
        if (!isMe) onSeen()
    }

    val isImage = message.type.equals("image", ignoreCase = true) ||
            message.content.startsWith("data:image")

    val isVoice = message.type.equals("voice", ignoreCase = true) ||
            message.content.startsWith("voice:")
    val displayText = if (isVoice) "Voice message" else message.content

    val imageBitmap = remember(message.content) {
        if (isImage) decodeBase64ToBitmap(message.content) else null
    }

    val timeText = remember(message.timestamp) {
        if (message.timestamp > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
        } else {
            ""
        }
    }

    // Luồng hiển thị chính
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        // Căn toàn bộ Column sang phải nếu là mình, sang trái nếu là người khác
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.widthIn(max = 300.dp),
            verticalAlignment = Alignment.Bottom,
            // Nếu là mình thì đẩy content sang cuối Row
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            // Avatar cho người khác (nằm bên trái tin nhắn)
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

            // Bubble tin nhắn
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp, // Đuôi tin nhắn hướng trái cho người khác
                    bottomEnd = if (isMe) 4.dp else 16.dp    // Đuôi tin nhắn hướng phải cho mình
                ),
                // Màu Teal cho mình, Xám nhạt cho người khác
                color = when {
                    isImage -> Color.Transparent
                    isMe -> TealPrimary
                    else -> Color(0xFFF1F1F1)
                },
                shadowElevation = if (isImage) 0.dp else 1.dp
            ) {
                when {
                    isImage -> {
                        if (imageBitmap != null) {
                            AsyncImage(
                                model = imageBitmap,
                                contentDescription = "Image",
                                modifier = Modifier
                                    .widthIn(max = 250.dp)
                                    .heightIn(max = 400.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = displayText,
                            // Chữ trắng trên nền Teal (của mình), chữ đen trên nền xám (người khác)
                            color = if (isMe) Color.White else Color.Black,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Hiển thị thời gian bên dưới bubble
        if (timeText.isNotEmpty()) {
            Text(
                text = timeText,
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(
                    top = 2.dp,
                    start = if (isMe) 0.dp else 40.dp, // Đẩy lề để tránh đè Avatar
                    end = if (isMe) 8.dp else 0.dp
                )
            )
        }
    }
}