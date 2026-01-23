package com.example.client.viewmodel

import android.util.Log
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

    // --- SỬA HÀM TÌM KIẾM TẠI ĐÂY ---
    fun searchUsers(phoneNumber: String) {
        val cleanQuery = phoneNumber.trim() // Xóa khoảng trắng 2 đầu
        if (cleanQuery.isEmpty()) return

        viewModelScope.launch {
            _isSearching.value = true
            try {
                // Gửi tham số "query" thay vì "phone"
                val response = apiService.searchUsers("Bearer $authToken", cleanQuery)
                if (response.isSuccessful) {
                    _searchResults.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("ContactVM", "Search failed: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }

    fun sendFriendRequest(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.sendFriendRequest("Bearer $authToken", FriendRequest(userId))
                if (response.success) {
                    val currentList = _searchResults.value.toMutableList()
                    currentList.removeIf { it.id == userId }
                    _searchResults.value = currentList
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

    fun acceptFriendRequest(userId: String, onSuccess: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.acceptFriendRequest("Bearer $authToken", FriendRequest(userId))
                if (response.success) {
                    fetchPendingRequests()
                    launch(Dispatchers.Main) { onSuccess() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}