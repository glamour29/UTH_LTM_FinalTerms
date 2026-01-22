// src/models/Message.js - ENHANCED VERSION
const mongoose = require('mongoose');

const reactionSchema = new mongoose.Schema({
    emoji: {
        type: String,
        required: true
    },
    userIds: [{
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User'
    }],
    count: {
        type: Number,
        default: 0
    }
}, { _id: false });

const messageSchema = new mongoose.Schema({
    roomId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Room',
        required: true
    },
    senderId: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },
    content: {
        type: String,
        required: true
    },
    type: {
        type: String,
        enum: ['TEXT', 'IMAGE', 'FILE', 'VOICE', 'CONTACT', 'SYSTEM'],
        default: 'TEXT'
    },
    status: {
        type: String,
        enum: ['sending', 'sent', 'delivered', 'seen'],
        default: 'sent'
    },
    isPinned: {
        type: Boolean,
        default: false
    },
    reactions: [reactionSchema],
    replyTo: {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'Message'
    },
    createdAt: {
        type: Date,
        default: Date.now
    }
}, { timestamps: true });

// Add methods for reactions
messageSchema.methods.addReaction = function(emoji, userId) {
    const existingReaction = this.reactions.find(r => r.emoji === emoji);
    
    if (existingReaction) {
        if (!existingReaction.userIds.includes(userId)) {
            existingReaction.userIds.push(userId);
            existingReaction.count = existingReaction.userIds.length;
        }
    } else {
        this.reactions.push({
            emoji,
            userIds: [userId],
            count: 1
        });
    }
    return this.save();
};

messageSchema.methods.removeReaction = function(emoji, userId) {
    const reactionIndex = this.reactions.findIndex(r => r.emoji === emoji);
    
    if (reactionIndex !== -1) {
        const reaction = this.reactions[reactionIndex];
        reaction.userIds = reaction.userIds.filter(id => id.toString() !== userId.toString());
        reaction.count = reaction.userIds.length;
        
        if (reaction.count === 0) {
            this.reactions.splice(reactionIndex, 1);
        }
    }
    
    return this.save();
};

const Message = mongoose.model('Message', messageSchema);
module.exports = Message;