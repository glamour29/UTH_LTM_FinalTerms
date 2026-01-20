package com.example.client.view.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.client.api.AuthService
import com.example.client.api.RetrofitClient
import com.example.client.models.LoginResponse
import com.example.client.models.RegisterRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit, // Đăng ký xong thì quay về Login
    onNavigateToLogin: () -> Unit  // Bấm nút "Đã có tk" thì về Login
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") } // Thêm cái này
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Đăng Ký Tài Khoản", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(30.dp))

        // Nhập Họ tên
        OutlinedTextField(
            value = fullName, onValueChange = { fullName = it },
            label = { Text("Họ và Tên") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Nhập Username
        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Tài khoản (Username)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Nhập Password
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Nút Đăng Ký
        Button(
            onClick = {
                if (username.isNotEmpty() && password.isNotEmpty() && fullName.isNotEmpty()) {
                    isLoading = true
                    performRegister(context, username, password, fullName,
                        onSuccess = {
                            isLoading = false
                            onRegisterSuccess()
                        },
                        onError = { isLoading = false }
                    )
                } else {
                    Toast.makeText(context, "Nhập đủ thông tin đi bạn ơi!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp))
            else Text("ĐĂNG KÝ NGAY", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Đã có tài khoản? Đăng nhập")
        }
    }
}

// Logic gọi API Register
fun performRegister(context: Context, user: String, pass: String, name: String, onSuccess: () -> Unit, onError: () -> Unit) {
    val authService = RetrofitClient.instance.create(AuthService::class.java)
    val request = RegisterRequest(user, pass, name)

    authService.register(request).enqueue(object : Callback<LoginResponse> {
        override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
            if (response.isSuccessful && response.body()?.success == true) {
                Toast.makeText(context, "Đăng ký thành công! Hãy đăng nhập.", Toast.LENGTH_LONG).show()
                onSuccess()
            } else {
                Toast.makeText(context, "Đăng ký thất bại: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                onError()
            }
        }

        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
            Toast.makeText(context, "Lỗi mạng: ${t.message}", Toast.LENGTH_SHORT).show()
            onError()
        }
    })
}