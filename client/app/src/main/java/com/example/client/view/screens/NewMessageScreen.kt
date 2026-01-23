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
import com.example.client.viewmodel.ContactViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    chatViewModel: ChatViewModel,
    contactViewModel: ContactViewModel,
    onBack: () -> Unit,
    onUserSelected: (User) -> Unit,
    onAddContact: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // State từ ViewModels
    val friends by chatViewModel.friends.collectAsState()
    val searchResults by contactViewModel.searchResults.collectAsState()
    val isSearchingRemote by contactViewModel.isSearching.collectAsState()

    // State quản lý tạo nhóm
    var selectedUserIds by remember { mutableStateOf(setOf<String>()) }
    var isGroupMode by remember { mutableStateOf(false) }
    var showGroupNameDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }

    // Logic tìm kiếm
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            contactViewModel.clearSearchResults()
            return@LaunchedEffect
        }
        delay(500)
        contactViewModel.searchUsers(searchQuery)
    }

    LaunchedEffect(Unit) {
        chatViewModel.refreshData()
    }

    val isSearchMode = searchQuery.isNotBlank()

    // Logic lọc danh sách hiển thị
    val displayedUsers = remember(searchQuery, searchResults, friends) {
        if (isSearchMode) {
            searchResults.filter { it.id != chatViewModel.currentUserId }
        } else {
            friends.filter { it.id != chatViewModel.currentUserId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (isGroupMode) "Thêm thành viên" else "Tin nhắn mới",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedUserIds.isNotEmpty()) {
                            Text(
                                "Đã chọn ${selectedUserIds.size} người",
                                fontSize = 12.sp,
                                color = TealPrimary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isGroupMode || selectedUserIds.isNotEmpty()) {
                            isGroupMode = false
                            selectedUserIds = emptySet()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(if (isGroupMode) Icons.Default.Close else Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (selectedUserIds.isNotEmpty()) {
                        TextButton(onClick = { showGroupNameDialog = true }) {
                            Text("Tiếp tục", color = TealPrimary, fontWeight = FontWeight.Bold)
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
            // Thanh tìm kiếm
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Tìm kiếm...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Quick Actions (Ẩn khi đang chọn nhóm)
            if (!isSearchMode && !isGroupMode) {
                QuickActionButtons(
                    onCreateGroup = { isGroupMode = true },
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
                        SectionHeader(if (isSearchMode) "Kết quả tìm kiếm" else "Bạn bè của bạn")
                    }

                    items(displayedUsers) { user ->
                        val isFriend = friends.any { it.id == user.id }
                        val isSelected = selectedUserIds.contains(user.id)

                        NewMessageContactRow(
                            contact = user,
                            isSelected = isSelected,
                            // Nút kết bạn chỉ hiện khi search thấy người lạ (không phải bạn)
                            showFriendRequestButton = isSearchMode && !isFriend,
                            onClick = {
                                if (isGroupMode || isSelected) {
                                    // Chế độ chọn thành viên nhóm
                                    if (isFriend) {
                                        selectedUserIds = if (isSelected) {
                                            selectedUserIds - user.id
                                        } else {
                                            selectedUserIds + user.id
                                        }
                                    }
                                } else {
                                    // Chế độ nhắn tin 1-1
                                    if (isFriend) {
                                        onUserSelected(user)
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

    // Dialog nhập tên nhóm
    if (showGroupNameDialog) {
        AlertDialog(
            onDismissRequest = { showGroupNameDialog = false },
            title = { Text("Tên nhóm mới") },
            text = {
                Column {
                    Text("Thành viên đã chọn: ${selectedUserIds.size}", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        placeholder = { Text("Nhập tên nhóm...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Gọi hàm tạo nhóm chat mới từ ChatViewModel
                        chatViewModel.createNewGroup(
                            name = groupName.ifBlank { "Nhóm mới" },
                            selectedMemberIds = selectedUserIds.toList()
                        )
                        showGroupNameDialog = false
                        onBack() // Quay về list room
                    },
                    enabled = groupName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Text("Tạo ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupNameDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TealVeryLight else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = TealLight) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        contact.username.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    contact.fullName.ifBlank { contact.username },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text("@${contact.username}", fontSize = 12.sp, color = Color.Gray)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = TealPrimary, modifier = Modifier.size(28.dp))
            } else if (showFriendRequestButton) {
                Button(
                    onClick = onSendFriendRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Kết bạn", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun QuickActionButtons(onCreateGroup: () -> Unit, onAddContact: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onCreateGroup() }
        ) {
            Surface(Modifier.size(50.dp), CircleShape, color = TealVeryLight) {
                Icon(Icons.Default.GroupAdd, null, Modifier.padding(12.dp), tint = TealPrimary)
            }
            Text("Tạo nhóm", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onAddContact() }
        ) {
            Surface(Modifier.size(50.dp), CircleShape, color = TealVeryLight) {
                Icon(Icons.Default.PersonAdd, null, Modifier.padding(12.dp), tint = TealPrimary)
            }
            Text("Thêm bạn", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun EmptySearchResult() {
    Text(
        "Không tìm thấy người dùng",
        Modifier
            .fillMaxWidth()
            .padding(32.dp),
        textAlign = TextAlign.Center,
        color = Color.Gray
    )
}

@Composable
fun EmptyContactList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.PeopleOutline, null, Modifier.size(48.dp), tint = Color.LightGray)
        Spacer(Modifier.height(8.dp))
        Text(
            "Bạn chưa có bạn bè nào để tạo nhóm",
            textAlign = TextAlign.Center,
            color = Color.Gray
        )
    }
}