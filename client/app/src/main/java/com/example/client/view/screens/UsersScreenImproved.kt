package com.example.client.view.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UsersScreenImproved(
    viewModel: ChatViewModel,
    pendingRequestsExternal: List<User>,
    onOpenChat: (roomId: String, roomName: String, isGroup: Boolean, memberCount: Int?) -> Unit,
    onOpenNewMessage: () -> Unit = {},
    onOpenProfile: () -> Unit,
    onOpenPendingRequests: () -> Unit = {}
) {
    // Sử dụng friends thay vì users để khớp với ChatViewModel mới
    val friends by viewModel.friends.collectAsState()
    val rooms by viewModel.rooms.collectAsState()

    var showCreateGroup by remember { mutableStateOf(false) }

    // Sắp xếp danh sách phòng chat theo thời gian và ghim
    val filteredRooms = remember(rooms) {
        rooms.filter { !it.isArchived }
            .sortedWith(compareByDescending<ChatRoom> { it.isPinned }
                .thenByDescending { it.lastUpdated })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tin nhắn", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            ChitzyBottomBar(onNewChatClick = onOpenNewMessage, onProfileClick = onOpenProfile)
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // 🔔 THANH THÔNG BÁO LỜI MỜI KẾT BẠN
            if (pendingRequestsExternal.isNotEmpty()) {
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
                            text = "Bạn có ${pendingRequestsExternal.size} lời mời kết bạn mới",
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
                        users = friends,
                        currentUserId = viewModel.currentUserId,
                        onRoomClick = { room ->
                            // Đồng bộ với hàm setActiveRoom trong ChatViewModel
                            viewModel.setActiveRoom(room.id, room.name)
                            onOpenChat(room.id, room.name, room.isGroup, if (room.isGroup) room.memberIds.size else null)
                        }
                    )
                } else {
                    EmptyChatsState(onNewChatClick = onOpenNewMessage)
                }
            }
        }
    }

    // Dialog tạo nhóm mới
    if (showCreateGroup) {
        CreateGroupDialog(
            users = friends.filter { it.id != viewModel.currentUserId },
            onDismiss = { showCreateGroup = false },
            onCreate = { name, memberIds ->
                val room = viewModel.createGroup(name, memberIds)
                onOpenChat(room.id, room.name, true, memberIds.size + 1)
                showCreateGroup = false
            }
        )
    }
}

@Composable
fun ChatList(
    rooms: List<ChatRoom>,
    users: List<User>,
    currentUserId: String,
    onRoomClick: (ChatRoom) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        items(rooms, key = { it.id }) { room ->
            ChatItem(
                room = room,
                users = users,
                currentUserId = currentUserId,
                onClick = { onRoomClick(room) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItem(
    room: ChatRoom,
    users: List<User>,
    currentUserId: String,
    onClick: () -> Unit
) {
    // Format thời gian từ timestamp để tránh lỗi chuỗi ISO dài
    val formattedTime = remember(room.lastUpdated) {
        if (room.lastUpdated > 0) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(room.lastUpdated))
        } else ""
    }

    val isOtherUserOnline = remember(users, room.memberIds) {
        if (room.isGroup) false
        else {
            val otherId = room.memberIds.find { it != currentUserId }
            users.find { it.id == otherId }?.isOnline ?: false
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = if (room.lastMessage.isBlank()) "Bắt đầu trò chuyện" else room.lastMessage,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ChatItemAvatar(room: ChatRoom, isOnline: Boolean) {
    Box {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (room.isGroup) Color(0xFFE0F2F1) else TealLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (room.isGroup) Icons.Default.Group else Icons.Default.Person,
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
fun EmptyChatsState(onNewChatClick: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
            Spacer(Modifier.height(16.dp))
            Text("Chưa có cuộc hội thoại nào", fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNewChatClick,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Bắt đầu chat ngay")
            }
        }
    }
}

@Composable
fun ChitzyBottomBar(onNewChatClick: () -> Unit, onProfileClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BottomBarItem(Icons.Default.Chat, "Chats", true, {})
            BottomBarItem(Icons.Default.AddCircle, "New", false, onNewChatClick)
            BottomBarItem(Icons.Default.Person, "Profile", false, onProfileClick)
        }
    }
}

@Composable
fun BottomBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 4.dp)
    ) {
        Icon(icon, null, tint = if (selected) TealPrimary else Color.Gray)
        Text(text = label, fontSize = 12.sp, color = if (selected) TealPrimary else Color.Gray)
    }
}

@Composable
private fun CreateGroupDialog(users: List<User>, onDismiss: () -> Unit, onCreate: (String, List<String>) -> Unit) {
    var groupName by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo nhóm mới") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Tên nhóm") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Text("Chọn thành viên", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                    items(users) { user ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedMembers.contains(user.id)) selectedMembers.remove(user.id)
                                    else selectedMembers.add(user.id)
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selectedMembers.contains(user.id), onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text(user.fullName.ifBlank { user.username })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(groupName, selectedMembers.toList()) },
                enabled = groupName.isNotBlank() && selectedMembers.isNotEmpty()
            ) { Text("Tạo") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}