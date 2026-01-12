// server.js
const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const cors = require('cors');
const connectDB = require('./src/config/db'); // Import file kết nối DB
const authRoutes = require('./src/routes/authRoutes'); // Import Routes API
const socketAuthMiddleware = require('./src/middlewares/socketAuth'); // <--- [MỚI] Import Middleware bảo vệ Socket
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

// 4. Khởi tạo Socket.IO
const io = new Server(server, {
    cors: {
        origin: "*", 
        methods: ["GET", "POST"]
    }
});

// ---> [QUAN TRỌNG] KÍCH HOẠT BẢO VỆ SOCKET <---
// Mọi kết nối socket phải có Token hợp lệ mới được đi qua
io.use(socketAuthMiddleware);

// 5. Lắng nghe sự kiện Socket (Chỉ chạy khi user đã qua bước kiểm tra Token)
io.on("connection", (socket) => {
    // Lấy thông tin user từ biến socket.user (do middleware gắn vào)
    console.log(`✅ User đã kết nối: ${socket.user.userId}`);
    console.log(`   Socket ID: ${socket.id}`);

    socket.on("disconnect", () => {
        console.log(`❌ User ${socket.user.userId} đã thoát.`);
    });
});

// 6. Chạy Server
const PORT = process.env.PORT || 3000;

server.listen(PORT, () => {
    console.log(`-----------------------------------`);
    console.log(`🚀 Server đang chạy tại: http://localhost:${PORT}`);
    console.log(`✅ API Auth sẵn sàng tại: http://localhost:${PORT}/api/auth/register`);
    console.log(`🔐 Socket Security: ON (Yêu cầu Token)`);
    console.log(`-----------------------------------`);
});