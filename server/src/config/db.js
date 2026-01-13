// src/config/db.js
const mongoose = require('mongoose');
require('dotenv').config(); // Load biến từ file .env

const connectDB = async () => {
    try {
        await mongoose.connect(process.env.MONGO_URI, {
            dbName : 'chat-app_db'
        });
        console.log('✅ MongoDB Connected Successfully!');
    } catch (error) {
        console.error('❌ MongoDB Connection Failed:', error.message);
        // Nếu lỗi DB thì dừng server luôn, vì không có DB không làm ăn gì được
        process.exit(1);
    }
};

module.exports = connectDB;