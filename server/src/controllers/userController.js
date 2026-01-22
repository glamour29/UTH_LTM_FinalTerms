const ChatService = require('../services/ChatService'); // Import Service
const User = require('../models/User'); // Import Model User để kiểm tra tồn tại

// 1. Lấy danh sách users
exports.getUsers = async (req, res) => {
    try {
        const users = await ChatService.getAllUsers();
        // Lọc bỏ chính mình
        const otherUsers = users.filter(u => u._id.toString() !== req.user.userId);
        res.json(otherUsers);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// 2. Cập nhật Profile
exports.updateProfile = async (req, res) => {
    try {
        const updatedUser = await ChatService.updateUserProfile(req.user.userId, req.body);
        res.json(updatedUser);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// 3. Gửi lời mời kết bạn (Fix lỗi "Không tìm thấy")
exports.sendFriendRequest = async (req, res) => {
    try {
        const { userId } = req.body; // ID người nhận
        const myId = req.user.userId;

        if (userId === myId) return res.status(400).json({ message: "Không thể kết bạn với chính mình" });

        // Kiểm tra người nhận có tồn tại không
        const targetUser = await User.findById(userId);
        if (!targetUser) {
            return res.status(404).json({ message: "Người dùng không tồn tại" });
        }

        const me = await User.findById(myId);
        if (me.friends.includes(userId)) {
            return res.status(400).json({ message: "Hai người đã là bạn bè" });
        }

        // Logic thêm vào pendingRequests
        await User.findByIdAndUpdate(userId, { $addToSet: { pendingRequests: myId } });

        // Socket thông báo (nếu có)
        const io = req.app.get('socketio');
        if (io) {
            io.to(userId).emit('new_friend_request', { from: myId });
        }

        res.status(200).json({ success: true, message: "Đã gửi lời mời!" });
    } catch (error) {
        console.error("Lỗi gửi kết bạn:", error);
        res.status(500).json({ message: error.message });
    }
};

// 4. Chấp nhận kết bạn & TẠO ROOM (Fix lỗi không tạo Room)
exports.acceptFriendRequest = async (req, res) => {
    try {
        const { userId } = req.body; // ID người gửi lời mời
        const myId = req.user.userId;

        console.log(`🤝 Chấp nhận kết bạn: ${myId} với ${userId}`);

        // A. Cập nhật danh sách bạn bè (2 chiều)
        await User.findByIdAndUpdate(myId, { 
            $addToSet: { friends: userId },
            $pull: { pendingRequests: userId } 
        });
        await User.findByIdAndUpdate(userId, { 
            $addToSet: { friends: myId } 
        });

        // B. GỌI SERVICE ĐỂ TẠO HOẶC LẤY ROOM (Đây là chỗ fix lỗi quan trọng)
        // ChatService đã có hàm createOrGetPrivateRoom chuẩn, ta dùng lại nó
        const room = await ChatService.createOrGetPrivateRoom(myId, userId);

        console.log(`✅ Phòng chat đã sẵn sàng: ${room._id}`);

        res.status(200).json({ 
            success: true, 
            message: "Kết bạn thành công!",
            roomId: room._id 
        });
    } catch (error) {
        console.error("❌ Lỗi chấp nhận kết bạn:", error);
        res.status(500).json({ message: error.message });
    }
};

// 5. Lấy danh sách lời mời
exports.getPendingRequests = async (req, res) => {
    try {
        const user = await User.findById(req.user.userId).populate('pendingRequests', 'username fullName avatarUrl phoneNumber');
        res.json(user ? user.pendingRequests : []);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// 6. Lấy danh sách bạn bè
exports.getFriends = async (req, res) => {
    try {
        const user = await User.findById(req.user.userId).populate('friends', 'username fullName avatarUrl phoneNumber');
        res.json(user ? user.friends : []);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};

// 7. Tìm kiếm user
exports.searchUsers = async (req, res) => {
    try {
        const query = req.query.q;
        if (!query) return res.json([]);
        
        // Tìm user trừ bản thân mình
        const users = await User.find({
            _id: { $ne: req.user.userId },
            $or: [
                { username: { $regex: query, $options: 'i' } },
                { phoneNumber: { $regex: query, $options: 'i' } },
                { fullName: { $regex: query, $options: 'i' } }
            ]
        }).select('username fullName avatarUrl phoneNumber');
        
        res.json(users);
    } catch (error) {
        res.status(500).json({ message: error.message });
    }
};