// src/models/User.js
const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
    username: {
        type: String,
        required: true,
        unique: true, // Không được trùng tên đăng nhập
        trim: true,
        minlength: 3
    },
    password: {
        type: String,
        required: true,
        minlength: 6
    },
    fullName: {
        type: String,
        default: ""
    },
    avatarUrl: {
        type: String,
        default: "https://i.imgur.com/6VBx3io.png" // Link ảnh mặc định nếu user chưa up avatar
    },
    isOnline: {
        type: Boolean,
        default: false // Mặc định offline, khi nào connect socket thì đổi thành true
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
});

// Ẩn mật khẩu đi khi trả dữ liệu về JSON (Bảo mật)
userSchema.methods.toJSON = function() {
    const user = this;
    const userObject = user.toObject();
    delete userObject.password; // Xóa trường password
    return userObject;
};

const User = mongoose.model('User', userSchema);
module.exports = User;