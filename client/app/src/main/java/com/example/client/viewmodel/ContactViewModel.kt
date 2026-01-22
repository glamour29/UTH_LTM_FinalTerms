package com.example.client.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.client.model.ApiService
import com.example.client.model.FriendRequest
import com.example.client.model.RetrofitClient
import com.example.client.model.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContactViewModel : ViewModel() {

    private val apiService: ApiService = RetrofitClient.instance
    private var authToken: String = ""

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _pendingRequests = MutableStateFlow<List<User>>(emptyList())
    val pendingRequests: StateFlow<List<User>> = _pendingRequests

    fun setToken(token: String) {
        this.authToken = token
    }

    // Tìm kiếm User
    fun searchUsers(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            try {
                // Gọi API search
                val results = apiService.searchUsers("Bearer $authToken", query)
                _searchResults.value = results
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    // Gửi lời mời kết bạn
    fun sendFriendRequest(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.sendFriendRequest("Bearer $authToken", FriendRequest(userId))
                if (response.success) {
                    // Xóa khỏi list tìm kiếm để cập nhật UI
                    val currentList = _searchResults.value.toMutableList()
                    currentList.removeIf { it.id == userId }
                    _searchResults.value = currentList

                    // Gọi callback thành công
                    launch(Dispatchers.Main) { onSuccess() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchPendingRequests() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requests = apiService.getPendingRequests("Bearer $authToken")
                _pendingRequests.value = requests
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    // Chấp nhận kết bạn - QUAN TRỌNG
    fun acceptFriendRequest(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Gọi API
                val response = apiService.acceptFriendRequest("Bearer $authToken", FriendRequest(userId))

                // 2. Nếu Server trả về success = true
                if (response.success) {
                    // Refresh lại list chờ
                    fetchPendingRequests()

                    // 3. Kích hoạt Callback để AppNavigation biết mà refresh ChatViewModel
                    launch(Dispatchers.Main) { onSuccess() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}