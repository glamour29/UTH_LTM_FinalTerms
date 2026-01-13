// src/routes/messageRoutes.js
const express = require('express');
const router = express.Router();
const messageController = require('../controllers/messageController');

// Định nghĩa API: GET /api/messages/:roomId
// :roomId là tham số động (bạn điền ID nào vào cũng được)
router.get('/:roomId', messageController.getMessages);

module.exports = router;