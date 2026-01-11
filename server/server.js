// server.js
const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const cors = require('cors');
const connectDB = require('./src/config/db'); // Import file kết nối DB
require('dotenv').config(); // Load biến môi trường từ .env

const app = express();
const server = http.createServer(app);

// 1. Kết nối Database
connectDB();

// 2. Cấu hình CORS (Để Android/Web gọi được API)
app.use(cors());
app.use(express.json()); // Cho phép server đọc dữ liệu JSON gửi lên

// 3. Khởi tạo Socket.IO
const io = new Server(server, {
    cors: {
        origin: "*", // Cho phép mọi kết nối (để test)
        methods: ["GET", "POST"]
    }
});

// 4. Lắng nghe sự kiện Socket (Tạm thời để test kết nối)
io.on("connection", (socket) => {
    console.log("⚡ Có người vừa kết nối: " + socket.id);

    socket.on("disconnect", () => {
        console.log("❌ User đã thoát: " + socket.id);
    });
});

// 5. Chạy Server (CHỈ KHAI BÁO 1 LẦN DUY NHẤT Ở ĐÂY)
const PORT = process.env.PORT || 3000;

server.listen(PORT, () => {
    console.log(`-----------------------------------`);
    console.log(`🚀 Server đang chạy tại: http://localhost:${PORT}`);
    console.log(`-----------------------------------`);
});