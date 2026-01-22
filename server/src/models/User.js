const mongoose = require('mongoose');
const userSchema = new mongoose.Schema({
    username: { type: String, required: true, unique: true, trim: true },
    password: { type: String, required: true },
    phoneNumber: { type: String, default: "", trim: true, index: true },
    fullName: { type: String, default: "" },
    avatarUrl: { type: String, default: "https://i.imgur.com/6VBx3io.png" },
    isOnline: { type: Boolean, default: false },
    // MỚI:
    friends: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    pendingRequests: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }]
}, { timestamps: true });

module.exports = mongoose.model('User', userSchema);