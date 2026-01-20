// src/config/db.js
const mongoose = require('mongoose');
require('dotenv').config();

const connectDB = async () => {
    try {
        const mongoUri = process.env.MONGO_URI || 'mongodb://localhost:27017/chat-app';
        
        await mongoose.connect(mongoUri, {
            dbName: 'chat-app_db'
        });
        console.log('[MongoDB] Connected Successfully!');
    } catch (error) {
        console.error('[MongoDB] Connection Failed:', error.message);
        console.log('[MongoDB] Running without database - data will not be saved');
        // Don't exit - allow server to run without database for testing
    }
};

module.exports = connectDB;