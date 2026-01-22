// src/services/ChatService.js
// Business logic for chat operations - hoc tu Ktor Clean Architecture

const { UserRepository, RoomRepository, MessageRepository } = require('../repositories');

class ChatService {
    
    // USER OPERATIONS
    async registerUser(userData) {
        const { username, email, password } = userData;
        
        const existingUser = await UserRepository.findByEmail(email);
        if (existingUser) {
            throw new Error('Email already registered');
        }
        
        const existingUsername = await UserRepository.findByUsername(username);
        if (existingUsername) {
            throw new Error('Username already taken');
        }
        
        return await UserRepository.create(userData);
    }
    
    async getAllUsers() {
        return await UserRepository.findAll({}, { select: '-password' });
    }
    
    async getOnlineUsers() {
        return await UserRepository.findOnlineUsers();
    }
    
    async setUserOnline(userId, isOnline) {
        return await UserRepository.updateOnlineStatus(userId, isOnline);
    }
    
    async updateUserProfile(userId, profileData) {
        return await UserRepository.updateProfile(userId, profileData);
    }
    
    // ROOM OPERATIONS
    async getUserRooms(userId) {
        return await RoomRepository.findUserRooms(userId);
    }
    
    async createOrGetPrivateRoom(userId1, userId2) {
        let room = await RoomRepository.findPrivateRoom(userId1, userId2);
        
        if (!room) {
            room = await RoomRepository.createPrivateRoom(userId1, userId2);
        }
        
        return room;
    }
    
    async createGroup(name, memberIds, adminId) {
        if (!name || name.trim() === '') {
            throw new Error('Group name is required');
        }
        
        if (!memberIds || memberIds.length === 0) {
            throw new Error('At least one member is required');
        }
        
        const allMembers = [...new Set([...memberIds, adminId])];
        
        return await RoomRepository.createGroup(name, allMembers, adminId);
    }
    
    async addMemberToRoom(roomId, userId) {
        const room = await RoomRepository.findById(roomId);
        if (!room) {
            throw new Error('Room not found');
        }
        
        if (!room.isGroup) {
            throw new Error('Cannot add members to private chat');
        }
        
        return await RoomRepository.addMember(roomId, userId);
    }
    
    async removeMemberFromRoom(roomId, userId, requesterId) {
        const room = await RoomRepository.findById(roomId);
        if (!room) {
            throw new Error('Room not found');
        }
        
        if (!room.isGroup) {
            throw new Error('Cannot remove members from private chat');
        }
        
        if (room.admin.toString() !== requesterId.toString()) {
            throw new Error('Only admin can remove members');
        }
        
        return await RoomRepository.removeMember(roomId, userId);
    }
    
    async toggleRoomPin(roomId) {
        return await RoomRepository.togglePin(roomId);
    }
    
    async toggleRoomMute(roomId) {
        return await RoomRepository.toggleMute(roomId);
    }
    
    async toggleRoomArchive(roomId) {
        return await RoomRepository.toggleArchive(roomId);
    }
    
    async addChannelToRoom(roomId, channelName, emoji) {
        return await RoomRepository.addChannel(roomId, channelName, emoji);
    }
    
    // MESSAGE OPERATIONS
    async getRoomMessages(roomId, options) {
        return await MessageRepository.findRoomMessages(roomId, options);
    }
    
    async sendMessage(messageData) {
        const { roomId, senderId, content, type } = messageData;
        
        if (!content || content.trim() === '') {
            throw new Error('Message content cannot be empty');
        }
        
        const room = await RoomRepository.findById(roomId);
        if (!room) {
            throw new Error('Room not found');
        }
        
        if (!room.members.includes(senderId)) {
            throw new Error('User is not a member of this room');
        }
        
        const message = await MessageRepository.createMessage({
            roomId,
            senderId,
            content,
            type: type || 'TEXT'
        });
        
        await RoomRepository.updateLastMessage(
            roomId,
            type === 'IMAGE' ? '📷 Image' : content,
            senderId
        );
        
        // --- SỬA Ở ĐÂY: Dùng hàm mới thay vì chuỗi lệnh gây lỗi ---
        return await MessageRepository.findByIdWithDetails(message._id);
        // ---------------------------------------------------------
    }
    
    async addReactionToMessage(messageId, emoji, userId) {
        return await MessageRepository.addReaction(messageId, emoji, userId);
    }
    
    async removeReactionFromMessage(messageId, emoji, userId) {
        return await MessageRepository.removeReaction(messageId, emoji, userId);
    }
    
    async pinMessage(messageId, roomId, userId) {
        const room = await RoomRepository.findById(roomId);
        if (!room) {
            throw new Error('Room not found');
        }
        
        if (room.isGroup && room.admin.toString() !== userId.toString()) {
            throw new Error('Only admin can pin messages in groups');
        }
        
        return await MessageRepository.pinMessage(messageId);
    }
    
    async unpinMessage(messageId, roomId, userId) {
        const room = await RoomRepository.findById(roomId);
        if (!room) {
            throw new Error('Room not found');
        }
        
        if (room.isGroup && room.admin.toString() !== userId.toString()) {
            throw new Error('Only admin can unpin messages in groups');
        }
        
        return await MessageRepository.unpinMessage(messageId);
    }
    
    async getPinnedMessages(roomId) {
        return await MessageRepository.getPinnedMessages(roomId);
    }
    
    async markMessageAsSeen(messageId) {
        return await MessageRepository.markAsSeen(messageId);
    }
    
    async getUnreadCount(roomId, userId) {
        return await MessageRepository.getUnreadCount(roomId, userId);
    }
}

module.exports = new ChatService();