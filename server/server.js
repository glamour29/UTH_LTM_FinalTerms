// server.js
const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const cors = require('cors');
const connectDB = require('./src/config/db'); // Import file kết nối DB
require('dotenv').config(); // Load biến môi trường từ .env

// ---> 1. IMPORT ROUTES MỚI TẠO <---
const authRoutes = require('./src/routes/authRoutes');

const app = express();
const server = http.createServer(app);

// 2. Kết nối Database
connectDB();

// 3. Cấu hình Middleware
app.use(cors()); // Cho phép gọi API từ Android/Web
app.use(express.json()); // Quan trọng: Để server đọc được dữ liệu JSON (req.body)

// ---> 4. KHAI BÁO ROUTES API <---
// Mọi request bắt đầu bằng /api/auth sẽ chạy vào file authRoutes
// Ví dụ: http://localhost:3000/api/auth/register
app.use('/api/auth', authRoutes);


// 5. Khởi tạo Socket.IO
const io = new Server(server, {
    cors: {
        origin: "*", 
        methods: ["GET", "POST"]
    }
});

// 6. Lắng nghe sự kiện Socket (Tạm thời để test)
io.on("connection", (socket) => {
    console.log("⚡ Có người vừa kết nối: " + socket.id);

    socket.on("disconnect", () => {
        console.log("❌ User đã thoát: " + socket.id);
    });
});

// 7. Chạy Server
const PORT = process.env.PORT || 3000;

server.listen(PORT, () => {
    console.log(`-----------------------------------`);
    console.log(`🚀 Server đang chạy tại: http://localhost:${PORT}`);
    console.log(`✅ API Auth sẵn sàng tại: http://localhost:${PORT}/api/auth/register`);
    console.log(`-----------------------------------`);
});