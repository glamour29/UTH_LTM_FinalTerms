package com.example.client.view.screens



import androidx.activity.compose.rememberLauncherForActivityResult // [MỚI] Để tạo launcher chọn ảnh
import androidx.activity.result.PickVisualMediaRequest // [MỚI] Yêu cầu chọn media
import androidx.activity.result.contract.ActivityResultContracts // [MỚI] Hợp đồng chọn ảnh
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons // [MỚI]
import androidx.compose.material.icons.filled.Add // [MỚI] Icon dấu +
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // [MỚI] Để lấy Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.client.view.components.MessageBubble
import com.example.client.viewmodel.ChatViewModel
import com.example.client.view.theme.YellowPrimary
import com.example.client.view.theme.TextBlack
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    // Collect state từ ViewModel
    val messages by viewModel.messages.collectAsState()
    var textState by remember { mutableStateOf("") }
    val typingUser by viewModel.typingUser.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current // Lấy context để xử lý ảnh

    // Bộ chọn ảnh (Photo Picker)
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.sendImage(context, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.joinRoom(viewModel.currentRoomId)
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastIndex = messages.size - 1
            // Nếu tin nhắn nhiều (do vừa load lịch sử) -> Cuộn ngay lập tức (scrollToItem)
            // Nếu tin nhắn ít (do chat mới) -> Cuộn mượt (animateScrollToItem)
            if (messages.size > 10 && listState.firstVisibleItemIndex < 2) {
                listState.scrollToItem(lastIndex)
            } else {
                listState.animateScrollToItem(lastIndex)
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Room", color = TextBlack, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = YellowPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Danh sách tin nhắn
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp), // Padding tổng thể cho list
                state = listState,
                reverseLayout = false
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isMe = message.senderId == viewModel.currentUserId,
                        // Khi hiển thị tin nhắn của người khác, báo cho server biết mình đã xem
                        onSeen = { viewModel.markAsSeen(message) }
                    )
                }
            }

            // [MỚI] Hiển thị Typing Indicator ngay trên ô nhập liệu
            if (typingUser != null) {
                Text(
                    text = "Người khác đang soạn tin...",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
            }

            // Khu vực nhập tin nhắn
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Icon(
                        imageVector = Icons.Default.Add, // Hoặc thay bằng icon Image/Attach
                        contentDescription = "Gửi ảnh",
                        tint = YellowPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
                OutlinedTextField(
                    value = textState,
                    onValueChange = {
                        textState = it
                        viewModel.onUserInputChanged(it) // [MỚI] Gọi hàm báo đang gõ
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Nhập tin nhắn...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YellowPrimary,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (textState.isNotBlank()) {
                            viewModel.sendMessage(textState)
                            textState = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = YellowPrimary,
                        contentColor = TextBlack
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text("Gửi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}