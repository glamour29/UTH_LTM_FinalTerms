// server.js - FINAL VERSION
const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const cors = require('cors');
const connectDB = require('./src/config/db');
const authRoutes = require('./src/routes/authRoutes');
const messageRoutes = require('./src/routes/messageRoutes'); // Route tin nhắn
const socketAuthMiddleware = require('./src/middlewares/socketAuth');
const chatSocket = require('./src/sockets/chatSocket');
require('dotenv').config();

const app = express();
const server = http.createServer(app);

// 1. Kết nối Database
connectDB();

// 2. Cấu hình Middleware HTTP
app.use(cors());
app.use(express.json());

// 3. Khai báo Routes API
app.use('/api/auth', authRoutes);
app.use('/api/messages', messageRoutes); // API lấy tin nhắn cũ

// 4. Khởi tạo Socket.IO
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

// 5. Kích hoạt bảo vệ Socket (Middleware)
io.use(socketAuthMiddleware);

// 6. Lắng nghe sự kiện Socket
io.on("connection", (socket) => {
    console.log(`✅ User đã kết nối: ${socket.user.userId}`);
    console.log(`   Socket ID: ${socket.id}`);

    // Kích hoạt tính năng Chat
    chatSocket(io, socket);

    socket.on("disconnect", () => {
        console.log(`❌ User ${socket.user.userId} đã thoát.`);
    });
});

// 7. Chạy Server
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`-----------------------------------`);
    console.log(`🚀 Server đang chạy tại: http://localhost:${PORT}`);
    console.log(`✅ API Auth sẵn sàng tại: http://localhost:${PORT}/api/auth/register`);
    console.log(`✅ API Messages sẵn sàng tại: http://localhost:${PORT}/api/messages/:roomId`);
    console.log(`🔐 Socket Security: ON (Yêu cầu Token)`);
    console.log(`-----------------------------------`);
});