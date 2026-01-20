// src/repositories/MessageRepository.js
const BaseRepository = require('./BaseRepository');
const Message = require('../models/Message');

class MessageRepository extends BaseRepository {
    constructor() {
        super(Message);
    }

    async findRoomMessages(roomId, options = {}) {
        const { limit = 100, skip = 0 } = options;
        
        return await this.findAll(
            { roomId },
            {
                populate: { path: 'senderId', select: 'username fullName avatarUrl' },
                sort: { createdAt: 1 },
                limit,
                skip
            }
        );
    }

    async createMessage(messageData) {
        return await this.create(messageData);
    }

    async updateMessage(messageId, content) {
        return await this.update(messageId, { 
            content,
            status: 'edited'
        });
    }

    async deleteMessage(messageId) {
        return await this.delete(messageId);
    }

    async addReaction(messageId, emoji, userId) {
        const message = await this.findById(messageId);
        if (!message) throw new Error('Message not found');
        
        const existingReaction = message.reactions.find(r => r.emoji === emoji);
        
        if (existingReaction) {
            if (!existingReaction.userIds.includes(userId)) {
                existingReaction.userIds.push(userId);
                existingReaction.count = existingReaction.userIds.length;
            }
        } else {
            message.reactions.push({
                emoji,
                userIds: [userId],
                count: 1
            });
        }
        
        return await message.save();
    }

    async removeReaction(messageId, emoji, userId) {
        const message = await this.findById(messageId);
        if (!message) throw new Error('Message not found');
        
        const reactionIndex = message.reactions.findIndex(r => r.emoji === emoji);
        
        if (reactionIndex !== -1) {
            const reaction = message.reactions[reactionIndex];
            reaction.userIds = reaction.userIds.filter(id => id.toString() !== userId.toString());
            reaction.count = reaction.userIds.length;
            
            if (reaction.count === 0) {
                message.reactions.splice(reactionIndex, 1);
            }
        }
        
        return await message.save();
    }

    async pinMessage(messageId) {
        return await this.update(messageId, { isPinned: true });
    }

    async unpinMessage(messageId) {
        return await this.update(messageId, { isPinned: false });
    }

    async getPinnedMessages(roomId) {
        return await this.findAll(
            { roomId, isPinned: true },
            { sort: { createdAt: -1 } }
        );
    }

    async markAsSeen(messageId) {
        return await this.update(messageId, { status: 'seen' });
    }

    async getUnreadCount(roomId, userId) {
        return await this.count({
            roomId,
            senderId: { $ne: userId },
            status: { $ne: 'seen' }
        });
    }

    async searchMessages(roomId, searchTerm) {
        return await this.findAll({
            roomId,
            content: { $regex: searchTerm, $options: 'i' }
        });
    }
}

module.exports = new MessageRepository();
