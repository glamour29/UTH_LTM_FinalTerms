const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const authMiddleware = require('../middlewares/authHTTP'); // 👈 Cần cái này để check Token

// Định nghĩa route: PUT /api/users/update
// authMiddleware sẽ chặn nếu không có Token
router.put('/update', authMiddleware, userController.updateProfile);

module.exports = router;