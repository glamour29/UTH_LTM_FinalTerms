// src/middlewares/socketAuth.js
const jwt = require('jsonwebtoken');

const socketAuthMiddleware = (socket, next) => {
    // 1. Lấy token từ client gửi lên (thường nằm trong handshake.auth)
    const token = socket.handshake.auth.token;

    if (!token) {
        return next(new Error("❌ Không tìm thấy Token! Bạn chưa đăng nhập."));
    }

    try {
        // 2. Giải mã Token để lấy thông tin user (verify)
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        
        // 3. Gắn thông tin user vào biến socket để dùng sau này
        // (Giống như đeo thẻ tên cho user)
        socket.user = decoded; 
        
        // 4. Cho phép đi tiếp
        next();
    } catch (err) {
        return next(new Error("❌ Token không hợp lệ hoặc đã hết hạn!"));
    }
};

module.exports = socketAuthMiddleware;