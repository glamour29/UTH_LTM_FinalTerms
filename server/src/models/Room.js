// src/models/Room.js - ENHANCED VERSION
const mongoose = require('mongoose');

const channelSchema = new mongoose.Schema({
    id: String,
    name: String,
    emoji: String
}, { _id: false });

const roomSchema = new mongoose.Schema({
    name: {
        type: String, 
        default: ""
    },
    description: {
        type: String,
        default: ""
    },
    avatarUrl: {
        type: String,
        default: ""
    },
    isGroup: {
        type: Boolean,
        default: false
    },
    members: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    }],
    admin: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User'
    },
    channels: [channelSchema], // For Discord/Slack-style channels
    activeChannel: {
        type: String,
        default: "general"
    },
    lastMessage: {
        type: String, 
        default: "Trò chuyện mới"
    },
    lastSenderId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User'
    },
    lastMessageTime: {
        type: Date,
        default: Date.now
    },
    // User-specific settings (will be handled differently in production)
    isPinned: {
        type: Boolean,
        default: false
    },
    isMuted: {
        type: Boolean,
        default: false
    },
    isArchived: {
        type: Boolean,
        default: false
    },
    unreadCount: {
        type: Number,
        default: 0
    }
}, { timestamps: true });

// Add method to add channel
roomSchema.methods.addChannel = function(channelName, emoji) {
    const channelId = channelName.toLowerCase().replace(/\s+/g, '-');
    
    if (!this.channels.find(c => c.id === channelId)) {
        this.channels.push({
            id: channelId,
            name: channelName,
            emoji: emoji || '💬'
        });
    }
    
    return this.save();
};

module.exports = mongoose.model('Room', roomSchema);