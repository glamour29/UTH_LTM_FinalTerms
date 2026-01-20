// server.js - FINAL VERSION
const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const cors = require('cors');
const connectDB = require('./src/config/db');
const authRoutes = require('./src/routes/authRoutes');
const messageRoutes = require('./src/routes/messageRoutes');
const roomRoutes = require('./src/routes/roomRoutes'); // <--- [MỚI] Import Route Room
const socketAuthMiddleware = require('./src/middlewares/socketAuth');
const chatSocket = require('./src/sockets/chatSocket');
const userRoutes = require('./src/routes/userRoutes');
require('dotenv').config();

const app = express();
const server = http.createServer(app);

// 1. Kết nối Database
connectDB();

// 2. Cấu hình Middleware HTTP
app.use(cors());
app.use(express.json({ limit: '50mb' })); 
app.use(express.urlencoded({ limit: '50mb', extended: true }));

// 3. Khai báo Routes API
app.use('/api/auth', authRoutes);
app.use('/api/messages', messageRoutes); // API tin nhắn cũ
app.use('/api/rooms', roomRoutes);       // <--- [MỚI] API quản lý phòng chat
app.use('/api/users', userRoutes); // <--- [MỚI] API quản lý user

// 4. Khởi tạo Socket.IO
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

// 5. Kích hoạt bảo vệ Socket
io.use(socketAuthMiddleware);

// 6. Lắng nghe sự kiện Socket
io.on("connection", (socket) => {
    console.log(`[Socket] User connected: ${socket.user.userId}`);
    console.log(`[Socket] Socket ID: ${socket.id}`);

    // Kích hoạt tính năng Chat
    chatSocket(io, socket);

    socket.on("disconnect", () => {
        console.log(`[Socket] User ${socket.user.userId} disconnected`);
    });
});

// 7. Chạy Server
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`-----------------------------------`);
    console.log(`[Server] Running at: http://localhost:${PORT}`);
    console.log(`[API] Auth: http://localhost:${PORT}/api/auth/register`);
    console.log(`[API] Rooms: http://localhost:${PORT}/api/rooms`);
    console.log(`[Security] Socket Auth: ON`);
    console.log(`-----------------------------------`);
});