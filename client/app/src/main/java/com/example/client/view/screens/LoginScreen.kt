package com.example.client.view.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.api.AuthService
import com.example.client.api.RetrofitClient
import com.example.client.models.LoginRequest
import com.example.client.models.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit, // Hàm callback để chuyển sang Home
    onNavigateToRegister: () -> Unit // Hàm callback để chuyển sang Register
) {
    // 1. Quản lý trạng thái (State)
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // Để hiện vòng quay loading

    val context = LocalContext.current

    // 2. Giao diện (UI)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Chat App Login",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Ô nhập Username
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Tài khoản") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ô nhập Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle Password")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nút Đăng nhập
        Button(
            onClick = {
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true // Bắt đầu quay
                    performLogin(context, username, password,
                        onSuccess = {
                            isLoading = false
                            onLoginSuccess() // Chuyển màn hình
                        },
                        onError = {
                            isLoading = false
                        }
                    )
                } else {
                    Toast.makeText(context, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading // Khóa nút khi đang load
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text(text = "ĐĂNG NHẬP", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nút chuyển sang Đăng ký
        TextButton(onClick = onNavigateToRegister) {
            Text("Chưa có tài khoản? Đăng ký ngay")
        }
    }
}

// 3. Logic gọi API (Tách ra cho gọn)
fun performLogin(
    context: Context,
    user: String,
    pass: String,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val authService = RetrofitClient.instance.create(AuthService::class.java)
    val request = LoginRequest(user, pass)

    authService.login(request).enqueue(object : Callback<LoginResponse> {
        override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
            if (response.isSuccessful) {
                val loginData = response.body()
                if (loginData?.token != null) {
                    // 🔥 LƯU TOKEN VÀO SHAREDPREFERENCES (QUAN TRỌNG)
                    val sharedPref = context.getSharedPreferences("ChatAppPrefs", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("TOKEN", loginData.token)
                        putString("USER_ID", loginData.userId)
                        putString("USERNAME", loginData.username)
                        apply()
                    }
                    Log.d("TOKEN_CUA_TUI", "Token là: ${loginData.token}")
                    Toast.makeText(context, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "Lỗi: ${loginData?.message}", Toast.LENGTH_SHORT).show()
                    onError()
                }
            } else {
                Toast.makeText(context, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                onError()
            }
        }

        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
            Toast.makeText(context, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            onError()
        }
    })
}
