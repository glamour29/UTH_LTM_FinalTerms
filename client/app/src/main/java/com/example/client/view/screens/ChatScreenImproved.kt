// kotlin
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.client.view.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.model.data.Message
import com.example.client.model.data.User
import com.example.client.model.data.ChatRoom
import com.example.client.view.components.MessageBubble
import com.example.client.view.theme.*
import com.example.client.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreenImprovedScreen(
    roomId: String,
    roomName: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val allUsers by viewModel.users.collectAsState()
    
    val currentRoom = rooms.find { it.id == roomId }
    
    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    var showGroupManage by remember { mutableStateOf(false) }

    LaunchedEffect(roomId) {
        viewModel.setActiveRoom(roomId, roomName)
        viewModel.markRoomAsRead(roomId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.sendImage(context, uri)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                roomName = currentRoom?.name ?: roomName,
                isGroup = currentRoom?.isGroup == true,
                onBack = onBack,
                onMoreClick = {
                    if (currentRoom?.isGroup == true) {
                        showGroupManage = true
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                state = listState
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isMe = message.senderId == viewModel.currentUserId,
                        onSeen = { viewModel.markAsSeen(message) }
                    )
                }
            }

            ChatInputArea(
                textState = textState,
                onTextChange = {
                    textState = it
                    viewModel.onUserInputChanged(it)
                },
                onSendClick = {
                    if (textState.isNotBlank()) {
                        viewModel.sendMessage(textState)
                        textState = ""
                    }
                },
                onImageClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }
    }

    if (showGroupManage && currentRoom != null) {
        GroupManagementDialog(
            room = currentRoom,
            allUsers = allUsers,
            onDismiss = { showGroupManage = false },
            onAddMember = { userId -> viewModel.addMember(roomId, userId) },
            onKickMember = { userId -> viewModel.kickMember(roomId, userId) },
            onRename = { newName -> viewModel.renameGroup(roomId, newName) },
            onTransferAdmin = { userId -> viewModel.transferAdmin(roomId, userId) },
            onLeave = { 
                viewModel.leaveRoom(roomId)
                onBack()
            }
        )
    }
}

@Composable
private fun ChatTopBar(
    roomName: String,
    isGroup: Boolean,
    onBack: () -> Unit,
    onMoreClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (isGroup) Color(0xFFE0F2F1) else TealLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isGroup) Icons.Default.Group else Icons.Default.Person,
                            contentDescription = null,
                            tint = TealPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = roomName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        actions = {
            IconButton(onClick = onMoreClick) {
                Icon(
                    if (isGroup) Icons.Default.Settings else Icons.Default.MoreVert, 
                    "Options", 
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun GroupManagementDialog(
    room: ChatRoom,
    allUsers: List<User>,
    onDismiss: () -> Unit,
    onAddMember: (String) -> Unit,
    onKickMember: (String) -> Unit,
    onRename: (String) -> Unit,
    onTransferAdmin: (String) -> Unit,
    onLeave: () -> Unit
) {
    var showAddUser by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf(room.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cài đặt nhóm", modifier = Modifier.weight(1f))
                IconButton(onClick = { showRename = true }) {
                    Icon(Icons.Default.Edit, "Sửa tên", modifier = Modifier.size(20.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 450.dp)) {
                if (showRename) {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Tên nhóm mới") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { 
                                onRename(newGroupName)
                                showRename = false
                            }) {
                                Icon(Icons.Default.Check, null, tint = TealPrimary)
                            }
                        }
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Text("Thành viên (${room.memberIds.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(room.memberIds) { memberId ->
                        val user = allUsers.find { it.id == memberId }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                user?.fullName?.ifBlank { user?.username } ?: memberId, 
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            // Nút Chuyển Admin
                            IconButton(onClick = { onTransferAdmin(memberId) }) {
                                Icon(Icons.Default.Star, "Admin", tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                            }
                            // Nút Xoá khỏi nhóm
                            IconButton(onClick = { onKickMember(memberId) }) {
                                Icon(Icons.Default.PersonRemove, "Kick", tint = Color.Red, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                
                TextButton(
                    onClick = { showAddUser = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Thêm thành viên")
                }
                
                TextButton(
                    onClick = onLeave,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.ExitToApp, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Rời nhóm")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Đóng") }
        }
    )

    if (showAddUser) {
        val nonMembers = allUsers.filter { !room.memberIds.contains(it.id) }
        AlertDialog(
            onDismissRequest = { showAddUser = false },
            title = { Text("Thêm thành viên") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(nonMembers) { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { 
                                onAddMember(user.id)
                                showAddUser = false
                            }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(user.fullName.ifBlank { user.username })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddUser = false }) { Text("Huỷ") }
            }
        )
    }
}

@Composable
private fun ChatInputArea(
    textState: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onImageClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Type message..",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                trailingIcon = {
                    IconButton(onClick = onImageClick) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                maxLines = 4
            )

            Spacer(Modifier.width(8.dp))

            FloatingActionButton(
                onClick = onSendClick,
                modifier = Modifier.size(48.dp),
                containerColor = TealPrimary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
