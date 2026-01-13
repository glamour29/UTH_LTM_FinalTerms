// src/controllers/messageController.js
const Message = require('../models/Message');

exports.getMessages = async (req, res) => {
    try {
        const { roomId } = req.params; // Lấy roomId từ URL

        // Tìm tin nhắn theo roomId
        const messages = await Message.find({ roomId })
            .sort({ createdAt: 1 }) // Sắp xếp: 1 là Tăng dần (Cũ trước, Mới sau)
            .populate('senderId', 'username avatarUrl fullName'); // "Nối bảng": Lấy thêm thông tin người gửi

        res.json(messages);

    } catch (error) {
        console.error(error);
        res.status(500).json({ message: "Lỗi server: " + error.message });
    }
};