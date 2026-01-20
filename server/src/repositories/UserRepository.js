// src/repositories/UserRepository.js
const BaseRepository = require('./BaseRepository');
const User = require('../models/User');

class UserRepository extends BaseRepository {
    constructor() {
        super(User);
    }

    async findByEmail(email) {
        return await this.findOne({ email });
    }

    async findByUsername(username) {
        return await this.findOne({ username });
    }

    async findOnlineUsers() {
        return await this.findAll({ isOnline: true });
    }

    async updateOnlineStatus(userId, isOnline) {
        return await this.update(userId, { isOnline });
    }

    async updateProfile(userId, { username, fullName, avatarUrl }) {
        const updateData = {};
        if (username) updateData.username = username;
        if (fullName) updateData.fullName = fullName;
        if (avatarUrl) updateData.avatarUrl = avatarUrl;
        
        return await this.update(userId, updateData);
    }

    async searchUsers(searchTerm) {
        return await this.findAll({
            $or: [
                { username: { $regex: searchTerm, $options: 'i' } },
                { fullName: { $regex: searchTerm, $options: 'i' } }
            ]
        });
    }

    async getUsersExcept(excludeUserId) {
        return await this.findAll(
            { _id: { $ne: excludeUserId } },
            { select: '-password' }
        );
    }
}

module.exports = new UserRepository();
