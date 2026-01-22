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
import com.example.client.model.repository.SocketRepository // Đảm bảo import đúng

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // 1. LẤY DỮ LIỆU TỪ SHAREDPREFS TRƯỚC
    val sharedPref = remember { context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE) }
    val savedToken = sharedPref.getString("TOKEN", null)
    val savedUserId = sharedPref.getString("USER_ID", "") ?: ""

    // 2. KHỞI TẠO REPOSITORY
    // Bạn nên dùng 'remember' để repository không bị tạo lại khi giao diện recompose
    val repository = remember { SocketRepository() }

    // 3. KHỞI TẠO VIEWMODEL VỚI FACTORY (Sử dụng repository và savedUserId đã khai báo ở trên)
    val chatViewModel: ChatViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(repository, savedUserId) as T
            }
        }
    )

    val contactViewModel: ContactViewModel = viewModel()

    // 4. THIẾT LẬP KẾT NỐI KHI APP MỞ
    LaunchedEffect(Unit) {
        if (savedToken != null && savedUserId.isNotBlank()) {
            chatViewModel.connect(savedToken, savedUserId)
            contactViewModel.setToken(savedToken)
            contactViewModel.fetchPendingRequests()
        }
    }

    val startDest = if (savedToken != null) "users" else "login"

    NavHost(navController = navController, startDestination = startDest) {
        // ... (Các composable khác giữ nguyên như bạn đã gửi)

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
                    if (isGroup && memberCount != null) {
                        navController.navigate("group/$roomId/${Uri.encode(roomName)}/$memberCount")
                    } else {
                        navController.navigate("chat/$roomId/${Uri.encode(roomName)}")
                    }
                },
                onOpenNewMessage = { navController.navigate("new_message") },
                onOpenProfile = { navController.navigate("profile") },
                onOpenPendingRequests = { navController.navigate("pending_requests") }
            )
        }

        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    with(sharedPref.edit()) {
                        clear()
                        apply()
                    }
                    chatViewModel.disconnect()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
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

        composable("new_message") {
            NewMessageScreen(
                chatViewModel = chatViewModel,
                contactViewModel = contactViewModel,
                onBack = { navController.popBackStack() },
                onUserSelected = { user ->
                    val room = chatViewModel.startPrivateChat(user)
                    navController.navigate("chat/${room.id}/${Uri.encode(room.name)}")
                },
                onAddContact = { navController.navigate("add_contact") },
                onCreateGroup = { name, ids ->
                    val room = chatViewModel.createGroup(name, ids)
                    navController.navigate("group/${room.id}/${Uri.encode(room.name)}/${ids.size + 1}")
                }
            )
        }

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
    }
}