package com.example.client.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onOpenProfile: () -> Unit,
    onOpenPendingRequests: () -> Unit = {} // Thêm tham số này để điều hướng
) {
    val users by viewModel.users.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    
    var showCreateGroup by remember { mutableStateOf(false) }
    var showRoomOptions by remember { mutableStateOf<ChatRoom?>(null) }

    val filteredRooms = remember(rooms) {
        rooms.filter { !it.isArchived }
            .sortedWith(compareByDescending<ChatRoom> { it.isPinned }.thenByDescending { it.lastUpdated })
    }

    Scaffold(
        topBar = { SimpleChatsTopBar() },
        bottomBar = { ChitzyBottomBar(onNewChatClick = onOpenNewMessage, onProfileClick = onOpenProfile) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            
            // 🔔 THANH THÔNG BÁO LỜI MỜI KẾT BẠN
            if (pendingRequests.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { onOpenPendingRequests() },
                    color = TealLight.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TealPrimary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Bạn có ${pendingRequests.size} lời mời kết bạn mới",
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TealPrimary)
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                if (filteredRooms.isNotEmpty()) {
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
                } else {
                    EmptyChatsState(onNewChatClick = onOpenNewMessage)
                }
            }
        }
    }

    // Dialogs giữ nguyên...
    if (showCreateGroup) {
        CreateGroupDialog(
            users = users.filter { it.id != viewModel.currentUserId },
            onDismiss = { showCreateGroup = false },
            onCreate = { name, memberIds ->
                val room = viewModel.createGroup(name, memberIds)
                onOpenChat(room.id, room.name, true, memberIds.size + 1)
                showCreateGroup = false
            }
        )
    }
}

// Các Composable phụ khác giữ nguyên từ file cũ của bạn...
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleChatsTopBar() {
    TopAppBar(
        title = { Text("Tin nhắn", fontWeight = FontWeight.Bold) }
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
    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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

@Composable
fun ChatItem(room: ChatRoom, users: List<User>, currentUserId: String, onClick: () -> Unit, onLongPress: () -> Unit) {
    val lastMessagePreview = getLastMessagePreview(room.lastMessage)
    val isOtherUserOnline = isUserOnline(room, users, currentUserId)

    Surface(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongPress),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            ChatItemAvatar(room, isOnline = isOtherUserOnline)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = room.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(text = lastMessagePreview.ifBlank { "Bấm để trò chuyện" }, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ChatItemAvatar(room: ChatRoom, isOnline: Boolean) {
    Box {
        Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = if (room.isGroup) Color(0xFFE0F2F1) else Color(0xFFB2DFDB)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (room.isGroup) Icons.Default.Group else Icons.Default.Person, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(28.dp))
            }
        }
        if (isOnline) {
            Surface(modifier = Modifier.size(14.dp).align(Alignment.BottomEnd), shape = CircleShape, color = OnlineGreen, border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)) {}
        }
    }
}

private fun getLastMessagePreview(lastMessage: String): String = if (lastMessage.length > 35) lastMessage.take(35) + "..." else lastMessage
private fun isUserOnline(room: ChatRoom, users: List<User>, currentUserId: String): Boolean {
    if (room.isGroup) return false
    val otherUserId = room.memberIds.find { it != currentUserId }
    return users.find { it.id == otherUserId }?.isOnline ?: false
}

@Composable
fun EmptyChatsState(onNewChatClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Chat, null, modifier = Modifier.size(60.dp), tint = Color.LightGray)
            Text("Chưa có cuộc hội thoại nào", fontWeight = FontWeight.Bold)
            Button(onClick = onNewChatClick, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)) {
                Text("Bắt đầu chat ngay")
            }
        }
    }
}

@Composable
fun ChitzyBottomBar(onNewChatClick: () -> Unit, onProfileClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            BottomBarItem(Icons.Default.Chat, "Chats", true, {})
            BottomBarItem(Icons.Default.Add, "New", false, onNewChatClick)
            BottomBarItem(Icons.Default.Person, "Profile", false, onProfileClick)
        }
    }
}

@Composable
fun BottomBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 4.dp)) {
        Icon(icon, null, tint = if (selected) TealPrimary else Color.Gray)
        Text(text = label, fontSize = 12.sp, color = if (selected) TealPrimary else Color.Gray)
    }
}

@Composable
private fun CreateGroupDialog(users: List<User>, onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    var groupName by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Tạo nhóm mới") }, text = {
        Column {
            OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Tên nhóm") })
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(users) { user ->
                    Row(Modifier.fillMaxWidth().clickable { if (selectedMembers.contains(user.id)) selectedMembers.remove(user.id) else selectedMembers.add(user.id) }.padding(8.dp)) {
                        Checkbox(checked = selectedMembers.contains(user.id), onCheckedChange = null)
                        Text(user.fullName.ifBlank { user.username })
                    }
                }
            }
        }
    }, confirmButton = { Button(onClick = { onCreate(groupName, selectedMembers.toList()) }) { Text("Tạo") } })
}
