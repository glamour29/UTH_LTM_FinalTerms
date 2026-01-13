// src/sockets/chatSocket.js
const Message = require('../models/Message');
const Room = require('../models/Room');

module.exports = (io, socket) => {
    // 1. Sự kiện: Tham gia vào phòng chat (User bấm vào 1 cuộc trò chuyện)
    socket.on('join_room', (roomId) => {
        socket.join(roomId); // Socket tham gia vào "kênh" riêng của phòng này
        console.log(`✅ User ${socket.user.userId} đã join vào phòng: ${roomId}`);
        
        // Gửi thông báo cho user biết đã join thành công
        socket.emit('joined_room', roomId);
    });

    // 2. Sự kiện: Gửi tin nhắn
    socket.on('send_message', async (data) => {
        // data gồm: { roomId, content, type }
        try {
            const { roomId, content, type } = data;
            const senderId = socket.user.userId; // Lấy ID từ token (đã xác thực)

            // A. Lưu tin nhắn vào MongoDB
            const newMessage = new Message({
                roomId,
                senderId,
                content,
                type: type || 'TEXT'
            });
            await newMessage.save();

            // B. Cập nhật tin nhắn cuối cùng cho Room (để hiện ở danh sách chat bên ngoài)
            await Room.findByIdAndUpdate(roomId, {
                lastMessage: content,
                lastMessageTime: new Date()
            });

            // C. Gửi tin nhắn ngay lập tức cho TẤT CẢ người trong phòng (Realtime)
            // io.to(roomId) -> Chỉ gửi cho những ai đang ở trong phòng này
            io.to(roomId).emit('receive_message', {
                _id: newMessage._id,
                content: newMessage.content,
                senderId: senderId,
                createdAt: newMessage.createdAt,
                type: newMessage.type
            });

            console.log(`📩 [${roomId}] ${senderId}: ${content}`);

        } catch (error) {
            console.error("❌ Lỗi gửi tin nhắn:", error.message);
            socket.emit('error', 'Gửi tin nhắn thất bại');
        }
    });

    // 3. Sự kiện: Đang gõ phím (Typing...) - Làm thêm cho xịn
    socket.on('typing', (roomId) => {
        socket.to(roomId).emit('user_typing', { userId: socket.user.userId });
    });

    socket.on('stop_typing', (roomId) => {
        socket.to(roomId).emit('user_stopped_typing', { userId: socket.user.userId });
    });
};