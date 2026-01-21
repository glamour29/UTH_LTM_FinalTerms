package com.example.client.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.model.data.ChatRoom
import com.example.client.model.data.User
import com.example.client.view.theme.*
import com.example.client.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun UsersScreenImproved(
    viewModel: ChatViewModel,
    onOpenChat: (roomId: String, roomName: String, isGroup: Boolean, memberCount: Int?) -> Unit,
    onOpenNewMessage: () -> Unit = {},
    onOpenProfile: () -> Unit // Thêm tham số này

) {
    val users by viewModel.users.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    var showCreateGroup by remember { mutableStateOf(false) }
    var showRoomOptions by remember { mutableStateOf<ChatRoom?>(null) }

    val filteredRooms = remember(rooms) {
        rooms.filter { !it.isArchived }
            .sortedWith(
                compareByDescending<ChatRoom> { it.isPinned }
                    .thenByDescending { it.lastUpdated }
            )
    }

    val onlineUsers = remember(users) {
        users.filter { it.isOnline && it.id != viewModel.currentUserId }
    }

    Scaffold(
        topBar = {
            SimpleChatsTopBar()
        },
        bottomBar = {
            ChitzyBottomBar(
                onNewChatClick = onOpenNewMessage,
                onProfileClick = onOpenProfile
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (onlineUsers.isNotEmpty()) {
                Column {
                    // Chat list
                    ChatList(
                        rooms = filteredRooms,
                        users = users,
                        currentUserId = viewModel.currentUserId,
                        onRoomClick = { room ->
                            viewModel.joinExistingRoom(room)
                            viewModel.markRoomAsRead(room.id)
                            onOpenChat(room.id, room.name, room.isGroup, if (room.isGroup) room.memberIds.size else null)
                        },
                        onRoomLongPress = { room -> showRoomOptions = room },
                        onCreateGroup = { showCreateGroup = true }
                    )
                }
            } else {
                // Empty state when no chats
                EmptyChatsState(
                    onNewChatClick = { showCreateGroup = true }
                )
            }
        }
    }

    // Dialogs
    if (showCreateGroup) {
        CreateGroupDialog(
            users = users.filter { it.id != viewModel.currentUserId },
            onDismiss = { showCreateGroup = false },
            onCreate = { name: String, memberIds: List<String> ->
                val room = viewModel.createGroup(name, memberIds)
                onOpenChat(room.id, room.name, true, memberIds.size + 1)
                showCreateGroup = false
            }
        )
    }

    showRoomOptions?.let { room ->
        RoomOptionsDialog(
            room = room,
            onDismiss = { showRoomOptions = null },
            onRead = {
                viewModel.markRoomAsRead(room.id)
                showRoomOptions = null
            },
            onPin = {
                if (room.isPinned) viewModel.unpinRoom(room.id)
                else viewModel.pinRoom(room.id)
                showRoomOptions = null
            },
            onArchive = {
                viewModel.archiveRoom(room.id)
                showRoomOptions = null
            },
            onMute = {
                if (room.isMuted) viewModel.unmuteRoom(room.id)
                else viewModel.muteRoom(room.id)
                showRoomOptions = null
            },
            onDelete = {
                viewModel.leaveRoom(room.id)
                showRoomOptions = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleChatsTopBar() {
    TopAppBar(
        title = { Text("Chats", fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun ChatList(
    rooms: List<ChatRoom>,
    users: List<User>,
    currentUserId: String,
    onRoomClick: (ChatRoom) -> Unit,
    onRoomLongPress: (ChatRoom) -> Unit,
    onCreateGroup: () -> Unit
) {
    if (rooms.isEmpty()) {
        EmptyChatsState(onNewChatClick = onCreateGroup)
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            items(rooms, key = { it.id }) { room ->
                ChatItem(
                    room = room,
                    users = users,
                    currentUserId = currentUserId,
                    onClick = { onRoomClick(room) },
                    onLongPress = { onRoomLongPress(room) }
                )
            }
        }
    }
}

@Composable
fun ChatItem(
    room: ChatRoom,
    users: List<User>,
    currentUserId: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val lastMessagePreview = getLastMessagePreview(room.lastMessage)
    val timeText = getTimeText(room.lastUpdated)
    val isOtherUserOnline = isUserOnline(room, users, currentUserId)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatItemAvatar(room, isOnline = isOtherUserOnline)
            Spacer(Modifier.width(12.dp))
            ChatItemContent(
                room = room,
                lastMessagePreview = lastMessagePreview,
                modifier = Modifier.weight(1f)
            )
            ChatItemMetadata(room, timeText)
        }
    }
}

@Composable
private fun ChatItemAvatar(room: ChatRoom, isOnline: Boolean) {
    Box {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (room.isGroup) Color(0xFFE0F2F1) else Color(0xFFB2DFDB)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (room.isGroup) Icons.Default.Group else Icons.Default.Person,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        if (isOnline) {
            Surface(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = OnlineGreen,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
            ) {}
        }
    }
}

@Composable
private fun ChatItemContent(
    room: ChatRoom,
    lastMessagePreview: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = room.name,
            fontWeight = if (room.unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = lastMessagePreview.ifBlank { "Tap to chat" },
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatItemMetadata(room: ChatRoom, timeText: String) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        if (timeText.isNotEmpty()) {
            Text(
                text = timeText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (room.unreadCount > 0 && !room.isMuted) {
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = CircleShape,
                color = TealPrimary
            ) {
                Text(
                    text = if (room.unreadCount > 9) "9+" else room.unreadCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

private fun getLastMessagePreview(lastMessage: String): String {
    return when {
        lastMessage.startsWith("data:image") -> "Photo"
        lastMessage.startsWith("voice:") -> "Voice message"
        else -> lastMessage.take(35) + if (lastMessage.length > 35) "..." else ""
    }
}

private fun getTimeText(lastUpdated: Long): String {
    return if (lastUpdated == 0L) "" else formatShortTimestamp(lastUpdated)
}

private fun isUserOnline(room: ChatRoom, users: List<User>, currentUserId: String): Boolean {
    if (room.isGroup) return false
    val otherUserId = room.memberIds.find { it != currentUserId }
    return users.find { it.id == otherUserId }?.isOnline ?: false
}

@Composable
fun EmptyChatsState(onNewChatClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Start a Conversation",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap the '+' button to message a friend",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ChitzyBottomBar(
    onNewChatClick: () -> Unit = {},
    onProfileClick: () -> Unit // Thêm callback
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item Chats
            BottomBarItem(
                icon = Icons.Default.Chat,
                label = "Chats",
                selected = true,
                onClick = {}
            )

            // Item New
            BottomBarItem(
                icon = Icons.Default.Add,
                label = "New",
                selected = false,
                onClick = onNewChatClick
            )

            BottomBarItem(
                icon = Icons.Default.Person, // Icon hình người
                label = "Profile",
                selected = false,
                onClick = onProfileClick
            )

        }
    }
}

@Composable
fun BottomBarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (selected) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun RoomOptionsDialog(
    room: ChatRoom,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    onMute: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat Options") },
        text = {
            Column {
                DialogOption(Icons.Default.DoneAll, "Mark as Read", onRead)
                DialogOption(
                    if (room.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                    if (room.isPinned) "Unpin" else "Pin",
                    onPin
                )
                DialogOption(Icons.Default.Archive, "Archive", onArchive)
                DialogOption(
                    if (room.isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    if (room.isMuted) "Unmute" else "Mute",
                    onMute
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DialogOption(Icons.Default.Delete, "Delete", onDelete, Color.Red)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DialogOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, color = color)
    }
}

@Composable
private fun CreateGroupDialog(
    users: List<User>,
    onDismiss: () -> Unit,
    onCreate: (String, List<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Group Name") },
                    placeholder = { Text("Enter group name...") }
                )
                Spacer(Modifier.height(16.dp))
                Text("Select Members", fontWeight = FontWeight.SemiBold)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(users) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedMembers.contains(user.id)) {
                                        selectedMembers.remove(user.id)
                                    } else {
                                        selectedMembers.add(user.id)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedMembers.contains(user.id),
                                onCheckedChange = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(user.fullName.ifBlank { user.username })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreate(groupName, selectedMembers.toList())
                    }
                },
                enabled = groupName.isNotBlank() && selectedMembers.isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatShortTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 172800_000 -> "Yesterday"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}