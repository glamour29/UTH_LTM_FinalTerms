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

    // Giả sử Token đã được lưu sẵn (ví dụ từ module Login của người khác)
    val sharedPref = context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE)
    val savedToken = sharedPref.getString("TOKEN", null)
    val savedUserId = sharedPref.getString("USER_ID", null)

    // Luôn bắt đầu từ màn hình users
    val startDest = "users"

    // Tự động kết nối Socket nếu có thông tin
    LaunchedEffect(savedToken, savedUserId) {
        if (savedToken != null && savedUserId != null) {
            chatViewModel.connect(savedToken, savedUserId)
        }
    }

    NavHost(navController = navController, startDestination = startDest) {

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
                onOpenNewMessage = { navController.navigate("new_message") }
            )
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
                onCreateGroup = { groupName, memberIds ->
                    val room = chatViewModel.createGroup(groupName, memberIds)
                    navController.navigate("group/${room.id}/${Uri.encode(room.name)}/${memberIds.size + 1}")
                }
            )
        }

        composable("add_contact") {
            AddNewContactScreen(
                onBack = { navController.popBackStack() },
                onSave = { _, _ -> navController.popBackStack() }
            )
        }

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
