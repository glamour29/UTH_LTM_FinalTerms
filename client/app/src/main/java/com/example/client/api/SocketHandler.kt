package com.example.client.api

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

object SocketHandler {
    lateinit var mSocket: Socket

    private const val SOCKET_URL = "http://10.0.2.2:3000/"

    @Synchronized
    fun setSocket(token: String) {
        try {
            val options = IO.Options()
            options.auth = mapOf("token" to "Bearer $token")

            // CẤU HÌNH CHỐNG TIME OUT
            options.timeout = 20000          // Chờ kết nối 20s
            options.reconnection = true      // Cho phép tự kết nối lại
            options.reconnectionAttempts = 5 // Thử lại tối đa 5 lần
            options.reconnectionDelay = 2000 // Mỗi lần thử cách nhau 2s
            options.transports = arrayOf("websocket")

            mSocket = IO.socket(SOCKET_URL, options)
        } catch (e: URISyntaxException) {
            Log.e("SOCKET_ERR", "Lỗi đường dẫn: ${e.message}")
        }
    }

    @Synchronized
    fun establishConnection() {
        if (!::mSocket.isInitialized) return

        mSocket.connect()

        // Lắng nghe sự kiện kết nối thành công
        mSocket.on(Socket.EVENT_CONNECT) {
            Log.d("SOCKET_STATUS", "✅ Đã kết nối Socket thành công! ID: ${mSocket.id()}")
        }

        // Lắng nghe lỗi kết nối
        mSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e("SOCKET_STATUS", "❌ Lỗi kết nối Socket: ${args[0]}")
        }
    }

    @Synchronized
    fun closeConnection() {
        if (::mSocket.isInitialized) {
            mSocket.disconnect()
        }
    }
}
