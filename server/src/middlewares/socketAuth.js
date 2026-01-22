const jwt = require('jsonwebtoken');

const socketAuthMiddleware = (socket, next) => {
    let token = socket.handshake.auth.token;

    if (!token) return next(new Error("❌ Thiếu Token!"));

    try {
        // Cắt bỏ Bearer nếu có
        if (token.startsWith('Bearer ')) {
            token = token.slice(7).trim();
        }

        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        socket.user = decoded; 
        next();
    } catch (err) {
        console.error("Socket Auth Error:", err.message);
        return next(new Error("❌ Token không hợp lệ!"));
    }
};
module.exports = socketAuthMiddleware;