package com.example.client.view.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.client.view.screens.*
import com.example.client.viewmodel.ChatViewModel
import com.example.client.viewmodel.ContactViewModel
import com.example.client.model.repository.SocketRepository

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 1. Lấy dữ liệu từ SharedPreferences
    val sharedPref = remember { context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE) }
    val savedToken = sharedPref.getString("TOKEN", null)
    val savedUserId = sharedPref.getString("USER_ID", "") ?: ""

    // 2. Khởi tạo Repository (Dùng remember để tránh khởi tạo lại)
    val repository = remember { SocketRepository() }

    // 3. Khởi tạo ViewModels
    val chatViewModel: ChatViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(repository, savedUserId) as T
            }
        }
    )
    val contactViewModel: ContactViewModel = viewModel()

    // 4. Thiết lập kết nối khi mở App
    LaunchedEffect(Unit) {
        if (savedToken != null && savedUserId.isNotBlank()) {
            chatViewModel.connect(savedToken, savedUserId)
            contactViewModel.setToken(savedToken)
            contactViewModel.fetchPendingRequests()
        }
    }

    val startDest = if (savedToken != null) "users" else "login"

    NavHost(navController = navController, startDestination = startDest) {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    val newToken = sharedPref.getString("TOKEN", null)
                    val newUserId = sharedPref.getString("USER_ID", "") ?: ""
                    if (newToken != null && newUserId.isNotBlank()) {
                        chatViewModel.connect(newToken, newUserId)
                        contactViewModel.setToken(newToken)
                        contactViewModel.fetchPendingRequests()
                    }
                    navController.navigate("users") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("users") {
            val pendingRequests by contactViewModel.pendingRequests.collectAsState()
            LaunchedEffect(Unit) {
                contactViewModel.fetchPendingRequests()
            }

            UsersScreenImproved(
                viewModel = chatViewModel,
                pendingRequestsExternal = pendingRequests,
                onOpenChat = { roomId, roomName, isGroup, memberCount ->
                    if (isGroup) {
                        // Điều hướng đến màn hình chat nhóm
                        navController.navigate("group/$roomId/${Uri.encode(roomName)}/${memberCount ?: 0}")
                    } else {
                        // Điều hướng đến màn hình chat 1-1
                        navController.navigate("chat/$roomId/${Uri.encode(roomName)}")
                    }
                },
                onOpenNewMessage = { navController.navigate("new_message") },
                onOpenProfile = { navController.navigate("profile") },
                onOpenPendingRequests = { navController.navigate("pending_requests") }
            )
        }

        composable("new_message") {
            NewMessageScreen(
                chatViewModel = chatViewModel,
                contactViewModel = contactViewModel,
                onBack = { navController.popBackStack() },
                onUserSelected = { user ->
                    val room = chatViewModel.startPrivateChat(user)
                    navController.navigate("chat/${room.id}/${Uri.encode(room.name)}")
                },
                onAddContact = { navController.navigate("add_contact") }
                // Chức năng tạo nhóm hiện tại đã được tích hợp gọi trực tiếp chatViewModel.createNewGroup
                // bên trong NewMessageScreen nên không cần truyền onCreateGroup qua param nếu bạn đã sửa file theo hướng đó.
            )
        }

        // ROUTE CHAT 1-1
        composable(
            route = "chat/{roomId}/{roomName}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ChatScreenImprovedScreen(
                roomId = backStackEntry.arguments?.getString("roomId") ?: "",
                roomName = Uri.decode(backStackEntry.arguments?.getString("roomName") ?: "Chat"),
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ROUTE CHAT NHÓM (Bổ sung để hết lỗi khi tạo nhóm xong)
        composable(
            route = "group/{roomId}/{roomName}/{memberCount}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType },
                navArgument("memberCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            GroupChatScreen(
                roomId = backStackEntry.arguments?.getString("roomId") ?: "",
                roomName = Uri.decode(backStackEntry.arguments?.getString("roomName") ?: "Nhóm"),
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    with(sharedPref.edit()) { clear(); apply() }
                    chatViewModel.disconnect()
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable("pending_requests") {
            PendingRequestsScreen(
                contactViewModel = contactViewModel,
                onFriendAccepted = { chatViewModel.refreshRooms() },
                onBack = { navController.popBackStack() }
            )
        }

        composable("add_contact") {
            AddNewContactScreen(
                viewModel = contactViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}