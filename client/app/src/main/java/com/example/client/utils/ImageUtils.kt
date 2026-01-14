package com.example.client.utils // Nhớ đổi tên package cho đúng với project của bạn

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

/**
 * Hàm chuyển đổi chuỗi Base64 thành Bitmap để hiển thị lên ảnh
 */
fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        // Cắt bỏ phần header "data:image/jpeg;base64," nếu server gửi kèm
        val pureBase64 = if (base64Str.contains(",")) {
            base64Str.substringAfter(",")
        } else {
            base64Str
        }

        // Giải mã chuỗi Base64 thành mảng byte
        val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)

        // Chuyển mảng byte thành Bitmap
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}