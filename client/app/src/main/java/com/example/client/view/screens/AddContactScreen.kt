package com.example.client.view.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.view.theme.*
import com.example.client.viewmodel.ChatViewModel
import com.example.client.model.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewContactScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    // Lắng nghe kết quả từ ViewModel
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    
    // Track if search has been performed at least once
    var searchPerformed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm bạn bè", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, null) }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
        ) {
            Text("Nhập số điện thoại để tìm kiếm", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { 
                    phoneNumber = it
                    if (it.isBlank()) {
                        viewModel.clearSearchResults()
                        searchPerformed = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ví dụ: 09123...") },
                leadingIcon = { Text(" +84", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp)) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !isSearching
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { 
                    if (phoneNumber.isNotBlank()) {
                        searchPerformed = true
                        viewModel.searchUsers(phoneNumber)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(16.dp),
                enabled = !isSearching && phoneNumber.isNotBlank()
            ) {
                if (isSearching) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("TÌM KIẾM", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))

            // HIỂN THỊ KẾT QUẢ TÌM KIẾM
            if (searchResults.isNotEmpty()) {
                Text("Kết quả tìm thấy:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                LazyColumn {
                    items(searchResults) { user ->
                        if (user.id != viewModel.currentUserId) {
                            SearchResultItem(
                                user = user,
                                onAddFriend = { 
                                    viewModel.sendFriendRequest(user.id)
                                    onBack()
                                }
                            )
                        }
                    }
                }
            } else if (searchPerformed && !isSearching) {
                // Chỉ hiện thông báo này khi đã search xong và không có kết quả
                Text("Không tìm thấy người dùng này", color = Color.Red, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SearchResultItem(user: User, onAddFriend: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = TealLight) {
                Box(contentAlignment = Alignment.Center) {
                    Text(user.username.take(1).uppercase(), fontWeight = FontWeight.Bold, color = TealPrimary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.fullName.ifBlank { user.username }, fontWeight = FontWeight.SemiBold)
                if (user.phoneNumber.isNotBlank()) {
                    Text(user.phoneNumber, fontSize = 12.sp, color = TealPrimary)
                } else {
                    Text("@${user.username}", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Button(
                onClick = onAddFriend,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("Kết bạn", fontSize = 12.sp)
            }
        }
    }
}
