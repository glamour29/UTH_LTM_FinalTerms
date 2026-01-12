// src/controllers/authController.js
const User = require('../models/User');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

// Hàm Đăng ký
exports.register = async (req, res) => {
    try {
        // 1. Lấy dữ liệu từ Client gửi lên
        const { username, password, fullName } = req.body;

        // 2. Kiểm tra xem user đã tồn tại chưa
        const existingUser = await User.findOne({ username });
        if (existingUser) {
            return res.status(400).json({ message: "Tài khoản đã tồn tại!" });
        }

        // 3. Mã hóa mật khẩu (Băm mật khẩu)
        // Ví dụ: "123456" -> "$2a$10$Xk9..."
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(password, salt);

        // 4. Tạo User mới
        const newUser = new User({
            username,
            password: hashedPassword, // Lưu mật khẩu đã mã hóa
            fullName
        });

        // 5. Lưu vào DB
        await newUser.save();

        // 6. Trả về thông báo thành công
        res.status(201).json({ 
            message: "Đăng ký thành công!",
            user: {
                _id: newUser._id,
                username: newUser.username,
                fullName: newUser.fullName
            }
        });

    } catch (error) {
        console.error(error);
        res.status(500).json({ message: "Lỗi Server: " + error.message });
    }
};

// Hàm Đăng nhập
exports.login = async (req, res) => {
    try {
        const { username, password } = req.body;

        // 1. Tìm user trong DB
        const user = await User.findOne({ username });
        if (!user) {
            return res.status(400).json({ message: "Sai tên đăng nhập hoặc mật khẩu!" });
        }

        // 2. So sánh mật khẩu (User gửi lên vs Mật khẩu mã hóa trong DB)
        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            return res.status(400).json({ message: "Sai tên đăng nhập hoặc mật khẩu!" });
        }

        // 3. Tạo Token (Vé thông hành)
        // Token này chứa ID của user, dùng để xác thực các request sau này
        const token = jwt.sign(
            { userId: user._id },
            process.env.JWT_SECRET,
            { expiresIn: '30d' } // Token sống 30 ngày mới hết hạn
        );

        // 4. Trả về Token và thông tin User
        res.json({
            message: "Đăng nhập thành công!",
            token,
            user: {
                _id: user._id,
                username: user.username,
                fullName: user.fullName,
                avatarUrl: user.avatarUrl
            }
        });

    } catch (error) {
        console.error(error);
        res.status(500).json({ message: "Lỗi Server: " + error.message });
    }
};