// File: app/src/main/java/com/example/client/view/screens/NewMessageScreen.kt
package com.example.client.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.model.data.User
import com.example.client.view.theme.*
import com.example.client.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke

// Extension giả lập (bạn có thể thay bằng logic thật nếu có)
val User.isOnline: Boolean
    get() = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onUserSelected: (User) -> Unit,
    onAddContact: () -> Unit,
    onCreateGroup: (String, List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    // coroutineScope removed (was unused)

    val friends by viewModel.friends.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var selectedUsers by remember { mutableStateOf(setOf<String>()) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }

    var isSearching by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Debounce search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            isSearching = false
            isLoading = false
            viewModel.clearSearchResults() // clear kết quả cũ nếu cần
            return@LaunchedEffect
        }

        isLoading = true
        isSearching = true
        delay(500) // debounce 500ms
        viewModel.searchUsers(searchQuery)
        isLoading = false
    }

    // Load data ban đầu
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    val displayedUsers = if (isSearching) searchResults else friends

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Tin nhắn mới",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (selectedUsers.isNotEmpty()) {
                            Text(
                                "${selectedUsers.size} người được chọn",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    if (selectedUsers.size >= 2) {
                        TextButton(onClick = { showCreateGroupDialog = true }) {
                            Text("Tạo nhóm", color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it.trim() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = {
                    Text(
                        "Tìm kiếm theo tên, username hoặc số điện thoại...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            QuickActionButtons(
                onCreateGroup = { showCreateGroupDialog = true },
                onAddContact = onAddContact
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Phần lời mời kết bạn đang chờ (chỉ hiển thị khi không search)
                if (!isSearching && pendingRequests.isNotEmpty()) {
                    item {
                        SectionHeader("Lời mời kết bạn đang chờ")
                    }
                    items(pendingRequests) { user ->
                        PendingRequestRow(
                            user = user,
                            onAccept = { viewModel.acceptFriendRequest(user.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Danh sách bạn bè hoặc kết quả tìm kiếm
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (displayedUsers.isEmpty()) {
                    item {
                        if (isSearching) {
                            EmptySearchResult()
                        } else {
                            EmptyContactList()
                        }
                    }
                } else {
                    if (!isSearching) {
                        item {
                            SectionHeader("Bạn bè")
                        }
                    }

                    items(displayedUsers) { user ->
                        val isFriend = friends.any { it.id == user.id }
                        val hasPending = pendingRequests.any { it.id == user.id }

                        NewMessageContactRow(
                            contact = user,
                            isSelected = selectedUsers.contains(user.id),
                            showFriendRequestButton = isSearching && !isFriend && !hasPending,
                            onClick = {
                                if (selectedUsers.isEmpty()) {
                                    // Chọn 1 người -> mở chat ngay
                                    onUserSelected(user)
                                } else {
                                    // Multi-select mode
                                    selectedUsers = if (selectedUsers.contains(user.id)) {
                                        selectedUsers - user.id
                                    } else {
                                        selectedUsers + user.id
                                    }
                                }
                            },
                            onSendFriendRequest = {
                                viewModel.sendFriendRequest(user.id)
                                // Optional: thông báo toast "Đã gửi lời mời" (bạn có thể thêm sau)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog tạo nhóm
    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            icon = { Icon(Icons.Default.GroupAdd, null, tint = TealPrimary) },
            title = { Text("Tạo nhóm mới") },
            text = {
                Column {
                    Text(
                        "Nhập tên nhóm (tối thiểu 2 thành viên)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        placeholder = { Text("Tên nhóm...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Đã chọn: ${selectedUsers.size} người",
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = groupName.ifBlank { "Nhóm mới" }
                        onCreateGroup(name, selectedUsers.toList())
                        showCreateGroupDialog = false
                        groupName = ""
                        selectedUsers = emptySet()
                    },
                    enabled = selectedUsers.size >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Tạo nhóm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun PendingRequestRow(
    user: User,
    onAccept: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = TealLight
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.username.firstOrNull()?.uppercase() ?: "U",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    user.fullName.ifBlank { user.username },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    "Đã gửi lời mời kết bạn",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Text("Chấp nhận")
            }
        }
    }
}

@Composable
fun NewMessageContactRow(
    contact: User,
    isSelected: Boolean,
    showFriendRequestButton: Boolean,
    onClick: () -> Unit,
    onSendFriendRequest: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) TealVeryLight else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = TealLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            contact.username.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                }

                if (contact.isOnline) {
                    Surface(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd),
                        shape = CircleShape,
                        color = OnlineGreen,
                        border = BorderStroke(3.dp, Color.White)
                    ) {}
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.fullName.ifBlank { contact.username },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (contact.isOnline) "Đang hoạt động" else "@${contact.username}",
                    fontSize = 14.sp,
                    color = if (contact.isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                isSelected -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Đã chọn",
                        tint = TealPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                showFriendRequestButton -> {
                    OutlinedButton(
                        onClick = onSendFriendRequest,
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text("Kết bạn")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySearchResult() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.PersonSearch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Không tìm thấy kết quả",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Hãy thử nhập tên, username hoặc số điện thoại khác",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun QuickActionButtons(
    onCreateGroup: () -> Unit,
    onAddContact: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Default.GroupAdd,
            label = "Tạo nhóm",
            onClick = onCreateGroup
        )
        QuickActionButton(
            icon = Icons.Default.PersonAdd,
            label = "Thêm bạn",
            onClick = onAddContact
        )
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = TealVeryLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = TealPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyContactList() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PersonOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Chưa có liên hệ nào",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Thêm bạn bè để bắt đầu trò chuyện",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}