// src/sockets/chatSocket.js
const Message = require('../models/Message');
const Room = require('../models/Room');

module.exports = (io, socket) => {
    
    // 1. Sự kiện: User tham gia vào phòng chat (Client gửi lên roomId)
    socket.on('join_room', (roomId) => {
        socket.join(roomId); // Gom user này vào một "nhóm riêng" theo roomId
        console.log(`✅ User ${socket.user.userId} đã join vào phòng: ${roomId}`);
        
        // Phản hồi lại cho Client biết là đã join xong
        socket.emit('joined_room', roomId);
    });

    // 2. Sự kiện: User gửi tin nhắn
    socket.on('send_message', async (data) => {
        // data nhận được: { roomId, content, type }
        try {
            const { roomId, content, type } = data;
            const senderId = socket.user.userId; // Lấy ID người gửi từ Token

            // A. Lưu tin nhắn vào Database (Message)
            const newMessage = new Message({
                roomId,
                senderId,
                content,
                type: type || 'TEXT'
            });
            await newMessage.save();

            // B. Cập nhật "Tin nhắn cuối cùng" cho Phòng (Room)
            // Để bên ngoài danh sách Inbox nó nhảy lên đầu và hiện nội dung mới nhất
            await Room.findByIdAndUpdate(roomId, {
                lastMessage: content,
                lastMessageTime: new Date()
            });

            // C. Gửi tin nhắn ngay lập tức cho TẤT CẢ người trong phòng (Realtime)
            // Gửi kèm đầy đủ thông tin để Client hiển thị
            io.to(roomId).emit('receive_message', {
                _id: newMessage._id,
                content: newMessage.content,
                senderId: senderId, // Frontend sẽ dùng ID này để map với avatar/tên
                createdAt: newMessage.createdAt,
                type: newMessage.type
            });

            console.log(`📩 [Room: ${roomId}] ${senderId} gửi: ${content}`);

        } catch (error) {
            console.error("❌ Lỗi gửi tin nhắn:", error.message);
            socket.emit('error', 'Gửi tin nhắn thất bại');
        }
    });

    // 3. Sự kiện: Đang gõ phím (Typing...)
    // user_typing gửi cho mọi người trừ chính mình (socket.to)
    socket.on('typing', (roomId) => {
        socket.to(roomId).emit('user_typing', { userId: socket.user.userId });
    });

    socket.on('stop_typing', (roomId) => {
        socket.to(roomId).emit('user_stopped_typing', { userId: socket.user.userId });
    });
};