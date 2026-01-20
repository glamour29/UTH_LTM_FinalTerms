// src/repositories/index.js
// Central export for all repositories

const UserRepository = require('./UserRepository');
const RoomRepository = require('./RoomRepository');
const MessageRepository = require('./MessageRepository');

module.exports = {
    UserRepository,
    RoomRepository,
    MessageRepository
};
