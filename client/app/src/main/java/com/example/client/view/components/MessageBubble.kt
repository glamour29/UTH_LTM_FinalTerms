package com.example.client.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
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

    val timeText = remember(message.timestamp, message.createdAt) {
        if (message.createdAt.isNotBlank()) {
            message.createdAt
        } else if (message.timestamp > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
        } else ""
    }

    // Main bubble row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        // Avatar for received messages
        if (!isMe) {
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Bottom),
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

        // Message content
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMe) 16.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 16.dp
                ),
                color = when {
                    isImage -> Color.Transparent
                    isMe -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
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
                                    .widthIn(max = 280.dp)
                                    .heightIn(max = 400.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isMe) 16.dp else 4.dp,
                                            bottomEnd = if (isMe) 4.dp else 16.dp
                                        )
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 100.dp)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    "Image failed to load",
                                    color = Color.Red,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = displayText,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            // Time (below bubble)
            if (timeText.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timeText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

