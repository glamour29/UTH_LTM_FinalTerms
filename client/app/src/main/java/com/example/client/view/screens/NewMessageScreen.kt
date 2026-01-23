package com.example.client.view.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.client.viewmodel.ContactViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    chatViewModel: ChatViewModel,
    contactViewModel: ContactViewModel,
    onBack: () -> Unit,
    onUserSelected: (User) -> Unit,
    onAddContact: () -> Unit,
    onCreateGroup: (String, List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // State từ ChatViewModel: Chứa danh sách bạn bè chính thức
    val friends by chatViewModel.friends.collectAsState()

    // State từ ContactViewModel: Chứa kết quả tìm kiếm (từ toàn bộ database)
    val searchResults by contactViewModel.searchResults.collectAsState()
    val isSearchingRemote by contactViewModel.isSearching.collectAsState()

    var selectedUsers by remember { mutableStateOf(setOf<String>()) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }

    // Logic debounce tìm kiếm
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            contactViewModel.clearSearchResults()
            return@LaunchedEffect
        }
        delay(500)
        contactViewModel.searchUsers(searchQuery)
    }

    // Refresh dữ liệu để đảm bảo danh sách bạn bè mới nhất từ Server
    LaunchedEffect(Unit) {
        chatViewModel.refreshData()
    }

    val isSearchMode = searchQuery.isNotBlank()

    // --- LOGIC LỌC DANH SÁCH: ĐẢM BẢO CHỈ HIỆN BẠN BÈ ---
    val displayedUsers = remember(searchQuery, searchResults, friends) {
        if (isSearchMode) {
            // Khi Search: Lấy từ kết quả tìm kiếm và loại bỏ chính mình
            searchResults.filter { it.id != chatViewModel.currentUserId }
        } else {
            // Khi Không Search: CHỈ lấy những người có trong danh sách friends của ChatViewModel
            // Nếu friends database trống, danh sách này sẽ trống (Empty)
            friends.filter { it.id != chatViewModel.currentUserId }
        }
    }

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
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                },
                actions = {
                    if (selectedUsers.size >= 2) {
                        TextButton(onClick = { showCreateGroupDialog = true }) {
                            Text("Tạo nhóm", color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = {
                    Text("Tìm kiếm người lạ để kết bạn...")
                },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            if (!isSearchMode) {
                QuickActionButtons(
                    onCreateGroup = { /* Handle if needed */ },
                    onAddContact = onAddContact
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (isSearchMode && isSearchingRemote) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            CircularProgressIndicator(color = TealPrimary)
                        }
                    }
                } else if (displayedUsers.isEmpty()) {
                    item {
                        if (isSearchMode) EmptySearchResult() else EmptyContactList()
                    }
                } else {
                    item {
                        SectionHeader(if (isSearchMode) "Kết quả tìm kiếm" else "Danh sách bạn bè")
                    }

                    items(displayedUsers) { user ->
                        // Kiểm tra xem người này có thực sự là bạn không
                        val isAlreadyFriend = friends.any { it.id == user.id }

                        NewMessageContactRow(
                            contact = user,
                            isSelected = selectedUsers.contains(user.id),
                            // Nút kết bạn chỉ hiện khi tìm thấy người lạ
                            showFriendRequestButton = isSearchMode && !isAlreadyFriend,
                            onClick = {
                                if (selectedUsers.isEmpty()) {
                                    // Chat ngay nếu là bạn bè
                                    if (isAlreadyFriend) {
                                        onUserSelected(user)
                                    }
                                } else {
                                    // Chế độ tạo nhóm: Chỉ cho chọn bạn bè
                                    if (isAlreadyFriend) {
                                        selectedUsers = if (selectedUsers.contains(user.id)) {
                                            selectedUsers - user.id
                                        } else {
                                            selectedUsers + user.id
                                        }
                                    }
                                }
                            },
                            onSendFriendRequest = {
                                contactViewModel.sendFriendRequest(user.id) {}
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
            title = { Text("Tạo nhóm mới") },
            text = {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    placeholder = { Text("Tên nhóm...") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateGroup(groupName.ifBlank { "Nhóm mới" }, selectedUsers.toList())
                        showCreateGroupDialog = false
                        selectedUsers = emptySet()
                    },
                    enabled = selectedUsers.size >= 2
                ) { Text("Tạo nhóm") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = TealPrimary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TealVeryLight else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = TealLight) {
                Box(contentAlignment = Alignment.Center) {
                    Text(contact.username.take(1).uppercase(), fontWeight = FontWeight.Bold, color = TealPrimary)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.fullName.ifBlank { contact.username }, fontWeight = FontWeight.SemiBold)
                Text("@${contact.username}", fontSize = 12.sp, color = Color.Gray)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = TealPrimary)
            } else if (showFriendRequestButton) {
                Button(onClick = onSendFriendRequest, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)) {
                    Text("Kết bạn", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun QuickActionButtons(onCreateGroup: () -> Unit, onAddContact: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onCreateGroup() }) {
            Surface(Modifier.size(50.dp), CircleShape, color = TealVeryLight) {
                Icon(Icons.Default.GroupAdd, null, Modifier.padding(12.dp), tint = TealPrimary)
            }
            Text("Tạo nhóm", fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onAddContact() }) {
            Surface(Modifier.size(50.dp), CircleShape, color = TealVeryLight) {
                Icon(Icons.Default.PersonAdd, null, Modifier.padding(12.dp), tint = TealPrimary)
            }
            Text("Thêm bạn", fontSize = 12.sp)
        }
    }
}

@Composable
fun EmptySearchResult() {
    Text("Không tìm thấy người dùng", Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = Color.Gray)
}

@Composable
fun EmptyContactList() {
    Text("Bạn chưa có bạn bè nào", Modifier.fillMaxWidth().padding(32.dp), textAlign = TextAlign.Center, color = Color.Gray)
}