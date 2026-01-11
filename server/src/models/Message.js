// src/models/Message.js
const mongoose = require('mongoose');

const messageSchema = new mongoose.Schema({
    roomId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Room', // Liên kết với bảng Room
        required: true
    },
    senderId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User', // Liên kết với bảng User
        required: true
    },
    content: {
        type: String,
        required: true
    },
    type: {
        type: String,
        enum: ['TEXT', 'IMAGE', 'SYSTEM'], // Loại tin nhắn
        default: 'TEXT'
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

const Message = mongoose.model('Message', messageSchema);
module.exports = Message;