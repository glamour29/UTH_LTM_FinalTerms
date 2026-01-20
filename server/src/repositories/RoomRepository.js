// src/repositories/RoomRepository.js
const BaseRepository = require('./BaseRepository');
const Room = require('../models/Room');

class RoomRepository extends BaseRepository {
    constructor() {
        super(Room);
    }

    async findByName(name) {
        return await this.findOne({ name });
    }

    async findUserRooms(userId) {
        return await this.findAll(
            { members: userId },
            {
                populate: [
                    { path: 'members', select: 'username fullName avatarUrl isOnline' },
                    { path: 'admin', select: 'username fullName' }
                ],
                sort: { lastMessageTime: -1 }
            }
        );
    }

    async findPrivateRoom(userId1, userId2) {
        return await this.findOne({
            isGroup: false,
            members: { $all: [userId1, userId2], $size: 2 }
        });
    }

    async createPrivateRoom(userId1, userId2) {
        return await this.create({
            isGroup: false,
            members: [userId1, userId2],
            name: ''
        });
    }

    async createGroup(name, members, adminId) {
        return await this.create({
            name,
            isGroup: true,
            members,
            admin: adminId
        });
    }

    async addMember(roomId, userId) {
        const room = await this.findById(roomId);
        if (!room) throw new Error('Room not found');
        
        if (!room.members.includes(userId)) {
            room.members.push(userId);
            return await room.save();
        }
        
        return room;
    }

    async removeMember(roomId, userId) {
        return await this.model.findByIdAndUpdate(
            roomId,
            { $pull: { members: userId } },
            { new: true }
        );
    }

    async updateLastMessage(roomId, message, senderId) {
        return await this.update(roomId, {
            lastMessage: message,
            lastSenderId: senderId,
            lastMessageTime: new Date()
        });
    }

    async addChannel(roomId, channelName, emoji) {
        const room = await this.findById(roomId);
        if (!room) throw new Error('Room not found');
        
        const channelId = channelName.toLowerCase().replace(/\s+/g, '-');
        
        const existingChannel = room.channels.find(c => c.id === channelId);
        if (existingChannel) {
            throw new Error('Channel already exists');
        }
        
        room.channels.push({
            id: channelId,
            name: channelName,
            emoji: emoji || '💬'
        });
        
        return await room.save();
    }

    async getPinnedRooms(userId) {
        return await this.findAll(
            { members: userId, isPinned: true },
            { sort: { lastMessageTime: -1 } }
        );
    }

    async togglePin(roomId) {
        const room = await this.findById(roomId);
        if (!room) throw new Error('Room not found');
        
        room.isPinned = !room.isPinned;
        return await room.save();
    }

    async toggleMute(roomId) {
        const room = await this.findById(roomId);
        if (!room) throw new Error('Room not found');
        
        room.isMuted = !room.isMuted;
        return await room.save();
    }

    async toggleArchive(roomId) {
        const room = await this.findById(roomId);
        if (!room) throw new Error('Room not found');
        
        room.isArchived = !room.isArchived;
        return await room.save();
    }
}

module.exports = new RoomRepository();
