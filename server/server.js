// server.js
const express = require('express');
const http = require('http');
const { Server } = require("socket.io");
const cors = require('cors');

const app = express();
const server = http.createServer(app);

// Cấu hình CORS để Client (Android/Web) gọi được mà không bị chặn
app.use(cors());

// Khởi tạo Socket.IO
const io = new Server(server, {
    cors: {
        origin: "*", // Cho phép mọi kết nối (để test cho dễ)
        methods: ["GET", "POST"]
    }
});

// Lắng nghe sự kiện kết nối (Handshake)
io.on("connection", (socket) => {
    console.log("⚡ Có người vừa kết nối: " + socket.id);

    // Sự kiện ngắt kết nối
    socket.on("disconnect", () => {
        console.log("❌ User đã thoát: " + socket.id);
    });
});

// Chạy server trên port 3000
const PORT = 3000;
server.listen(PORT, () => {
    console.log(`-----------------------------------`);
    console.log(`🚀 Server đang chạy tại: http://localhost:${PORT}`);
    console.log(`-----------------------------------`);
});