package com.example.client.view.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.client.view.screens.*
import com.example.client.viewmodel.ChatViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()
    val context = LocalContext.current

    // Lấy Token đã lưu
    val sharedPref = context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE)
    val savedToken = sharedPref.getString("TOKEN", null)
    val savedUserId = sharedPref.getString("USER_ID", null)

    //LOGIC QUAN TRỌNG: Nếu chưa có Token thì vào Login, có rồi thì vào Users
    val startDest = if (savedToken != null) "users" else "login"

    // Tự động kết nối Socket nếu có thông tin
    LaunchedEffect(savedToken, savedUserId) {
        if (savedToken != null && savedUserId != null) {
            chatViewModel.connect(savedToken, savedUserId)
        }
    }

    NavHost(navController = navController, startDestination = startDest) {

        // 1. Màn hình Đăng Nhập
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("users") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // 2. Màn hình Đăng Ký
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("login") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // 3. Màn hình Danh sách User (ĐÃ GỘP VÀ SỬA LỖI)
        composable("users") {
            UsersScreenImproved(
                viewModel = chatViewModel,
                // Logic mở khung chat
                onOpenChat = { roomId, roomName, isGroup, memberCount ->
                    if (isGroup && memberCount != null) {
                        navController.navigate("group/$roomId/${Uri.encode(roomName)}/$memberCount")
                    } else {
                        navController.navigate("chat/$roomId/${Uri.encode(roomName)}")
                    }
                },
                // Logic mở màn hình tin nhắn mới
                onOpenNewMessage = { navController.navigate("new_message") },
                // Logic mở Profile (SỬA LỖI Ở ĐÂY)
                onOpenProfile = { navController.navigate("profile") }
            )
        }

        // 4. Màn hình Profile
        composable("profile") {
            ProfileScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true } // Xóa hết stack khi logout
                    }
                }
            )
        }

        // 5. Màn hình Tin nhắn mới
        composable("new_message") {
            NewMessageScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() },
                onUserSelected = { user ->
                    val room = chatViewModel.startPrivateChat(user)
                    navController.navigate("chat/${room.id}/${Uri.encode(room.name)}")
                },
                onAddContact = { navController.navigate("add_contact") },
                onCreateGroup = { groupName, memberIds ->
                    val room = chatViewModel.createGroup(groupName, memberIds)
                    navController.navigate("group/${room.id}/${Uri.encode(room.name)}/${memberIds.size + 1}")
                }
            )
        }

        // 6. Màn hình Thêm bạn
        composable("add_contact") {
            AddNewContactScreen(
                onBack = { navController.popBackStack() },
                onSave = { _, _ -> navController.popBackStack() }
            )
        }

        // 7. Màn hình Chat 1-1
        composable(
            route = "chat/{roomId}/{roomName}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val roomName = backStackEntry.arguments?.getString("roomName") ?: "Chat"
            ChatScreenImprovedScreen(
                roomId = roomId,
                roomName = Uri.decode(roomName),
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // 8. Màn hình Chat Nhóm
        composable(
            route = "group/{roomId}/{roomName}/{memberCount}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("roomName") { type = NavType.StringType },
                navArgument("memberCount") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val roomName = backStackEntry.arguments?.getString("roomName") ?: "Group"
            ChatScreenImprovedScreen(
                roomId = roomId,
                roomName = Uri.decode(roomName),
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}