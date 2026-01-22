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

    val sharedPref = context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE)
    val savedToken = sharedPref.getString("TOKEN", null)
    val savedUserId = sharedPref.getString("USER_ID", null)

    LaunchedEffect(Unit) {
        if (savedToken != null && savedUserId != null) {
            chatViewModel.connect(savedToken, savedUserId)
        }
    }

    val startDest = if (savedToken != null) "users" else "login"

    NavHost(navController = navController, startDestination = startDest) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    val newToken = sharedPref.getString("TOKEN", null)
                    val newUserId = sharedPref.getString("USER_ID", null)
                    if (newToken != null && newUserId != null) {
                        chatViewModel.connect(newToken, newUserId)
                    }
                    navController.navigate("users") { 
                        popUpTo("login") { inclusive = true } 
                    }
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }

        composable("users") {
            UsersScreenImproved(
                viewModel = chatViewModel,
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
                    // 1. Xóa sạch bộ nhớ
                    with(sharedPref.edit()) {
                        clear()
                        apply()
                    }
                    // 2. Ngắt kết nối socket
                    chatViewModel.disconnect()
                    
                    // 3. Quay về màn hình Login và XÓA TOÀN BỘ lịch sử màn hình cũ
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("pending_requests") {
            PendingRequestsScreen(
                viewModel = chatViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("add_contact") {
            AddNewContactScreen(viewModel = chatViewModel, onBack = { navController.popBackStack() })
        }

        composable("new_message") {
            NewMessageScreen(
                viewModel = chatViewModel,
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
            arguments = listOf(navArgument("roomId") { type = NavType.StringType }, navArgument("roomName") { type = NavType.StringType })
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
