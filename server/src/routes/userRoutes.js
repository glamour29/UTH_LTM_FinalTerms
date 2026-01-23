const express = require("express");
const router = express.Router();
const userController = require("../controllers/userController");
const authMiddleware = require("../middlewares/authHTTP");

// 1. Thêm route GET này để xử lý tìm kiếm (api/users?search=...)
// Quan trọng: Phải là router.get và đường dẫn là '/'
router.get("/", authMiddleware, userController.getUsers);
router.get("/search", authMiddleware, userController.searchUsers);

// 2. Giữ nguyên route update cũ của bạn
router.put("/update", authMiddleware, userController.updateProfile);


// Lấy danh sách bạn bè
router.get('/friends', authMiddleware, userController.getFriends);

// Lấy danh sách lời mời kết bạn đang chờ
router.get('/friends/pending', authMiddleware, userController.getPendingRequests);

// Gửi lời mời kết bạn
router.post('/friends/request', authMiddleware, userController.sendFriendRequest);

// Chấp nhận kết bạn
router.post('/friends/accept', authMiddleware, userController.acceptFriendRequest);

module.exports = router;
