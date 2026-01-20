// File: `app/src/main/java/com/example/client/view/screens/NewMessageScreen.kt`
package com.example.client.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.model.data.User
import com.example.client.view.theme.*
import com.example.client.viewmodel.ChatViewModel
// Public safe default extension so UI code can compile without creating a new file.
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
    val users by viewModel.users.collectAsState()
    var selectedUsers by remember { mutableStateOf(setOf<String>()) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var memberPickerExpanded by remember { mutableStateOf(false) }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) {
            users
        } else {
            users.filter { user ->
                user.username.contains(searchQuery, ignoreCase = true) ||
                        user.fullName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val openCreateGroup = { showCreateGroupDialog = true }

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
                        Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    if (selectedUsers.size >= 2) {
                        TextButton(onClick = openCreateGroup) {
                            Text("Tạo nhóm", color = TealPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = {
                    Text(
                        "Tìm kiếm theo tên hoặc số điện thoại...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            QuickActionButtons(
                onCreateGroup = openCreateGroup,
                onAddContact = onAddContact
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

            if (filteredUsers.isEmpty()) {
                EmptyContactList()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    val onlineUsers = filteredUsers.filter { it.isOnline }
                    if (onlineUsers.isNotEmpty() && searchQuery.isBlank()) {
                        item {
                            Text(
                                "Đang hoạt động",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(onlineUsers) { user ->
                            NewMessageContactRow(
                                contact = user,
                                isSelected = selectedUsers.contains(user.id),
                                onClick = {
                                    if (selectedUsers.isEmpty()) {
                                        onUserSelected(user)
                                    } else {
                                        selectedUsers = if (selectedUsers.contains(user.id)) selectedUsers - user.id
                                        else selectedUsers + user.id
                                    }
                                }
                            )
                        }
                    }

                    val allContacts = if (searchQuery.isBlank()) {
                        filteredUsers.filter { !it.isOnline }
                    } else {
                        filteredUsers
                    }

                    if (allContacts.isNotEmpty()) {
                        if (onlineUsers.isNotEmpty() && searchQuery.isBlank()) {
                            item {
                                Text(
                                    "Tất cả liên hệ",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        items(allContacts) { user ->
                            NewMessageContactRow(
                                contact = user,
                                isSelected = selectedUsers.contains(user.id),
                                onClick = {
                                    if (selectedUsers.isEmpty()) {
                                        onUserSelected(user)
                                    } else {
                                        selectedUsers = if (selectedUsers.contains(user.id)) selectedUsers - user.id
                                        else selectedUsers + user.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            onDismissRequest = { showCreateGroupDialog = false },
            icon = { Icon(Icons.Default.GroupAdd, null, tint = TealPrimary) },
            title = { Text("Tạo nhóm mới") },
            text = {
                Column {
                    Text(
                        "Nhập tên nhóm và chọn ít nhất 2 thành viên.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        placeholder = { Text("Tên nhóm") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = memberPickerExpanded,
                        onExpandedChange = { memberPickerExpanded = !memberPickerExpanded }
                    ) {
                        OutlinedTextField(
                            value = "Đã chọn: ${selectedUsers.size}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Thành viên") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = memberPickerExpanded
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = memberPickerExpanded,
                            onDismissRequest = { memberPickerExpanded = false }
                        ) {
                            users.forEach { user ->
                                val isSelected = selectedUsers.contains(user.id)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    selectedUsers = if (isSelected) selectedUsers - user.id
                                                    else selectedUsers + user.id
                                                }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(user.fullName.ifBlank { user.username })
                                        }
                                    },
                                    onClick = {
                                        selectedUsers = if (isSelected) {
                                            selectedUsers - user.id
                                        } else {
                                            selectedUsers + user.id
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (selectedUsers.size < 2) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Chọn ít nhất 2 người.",
                            fontSize = 12.sp,
                            color = ErrorRed
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = groupName.ifBlank { "New Group" }
                        onCreateGroup(name, selectedUsers.toList())
                        showCreateGroupDialog = false
                        groupName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    enabled = selectedUsers.size >= 2
                ) {
                    Text("Tạo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGroupDialog = false }) {
                    Text("Huỷ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
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
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Default.Group,
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
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = TealVeryLight
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = TealPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NewMessageContactRow(
    contact: User,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = if (isSelected) TealVeryLight else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = TealLight
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            contact.username.firstOrNull()?.uppercase() ?: "U",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                }
                if (contact.isOnline) {
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

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.fullName.ifBlank { contact.username },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (contact.isOnline) "Đang hoạt động" else "@${contact.username}",
                    fontSize = 13.sp,
                    color = if (contact.isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = TealPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .size(24.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {}
            }
        }
    }
}

@Composable
fun EmptyContactList() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.PersonOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Không có liên hệ nào",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Hãy thêm bạn bè để bắt đầu trò chuyện",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}