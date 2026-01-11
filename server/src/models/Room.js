// src/models/Room.js
const mongoose = require('mongoose');

const roomSchema = new mongoose.Schema({
    name: {
        type: String, // Tên nhóm (nếu là group chat)
        default: "" 
    },
    isGroup: {
        type: Boolean,
        default: false // False = chat 1-1, True = chat nhóm
    },
    members: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User' // Danh sách id của các thành viên trong nhóm
    }],
    admin: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User' // Trưởng nhóm (người tạo group)
    },
    lastMessage: {
        type: String, // Lưu text tin nhắn cuối cùng để hiển thị ở danh sách cho nhanh
        default: ""
    },
    lastMessageTime: {
        type: Date,
        default: Date.now
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

const Room = mongoose.model('Room', roomSchema);
module.exports = Room;