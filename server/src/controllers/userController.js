const User = require('../models/User'); // Nhớ trỏ đúng đường dẫn đến Model User của bạn

// Hàm cập nhật thông tin (Avatar, Name...)
exports.updateProfile = async (req, res) => {
  try {
    // 1. Lấy ID user từ Token (Middleware xác thực sẽ gắn user vào req)
    const userId = req.user.userId || req.user.id || req.user._id;

    if (!userId) {
        return res.status(401).json({ success: false, message: "Token không chứa ID User!" });
    }
    
    // 2. Lấy dữ liệu Kiên gửi lên
    const { fullName, avatar } = req.body;

    // 3. Tìm và Update trong Database
    // $set: Chỉ update những trường có gửi lên, trường khác giữ nguyên
    const updatedUser = await User.findByIdAndUpdate(
      userId,
      { 
        $set: { 
          fullName: fullName,
          avatar: avatar 
        } 
      },
      { new: true } // Trả về data mới sau khi update
    ).select('-password'); // Không trả về password

    if (!updatedUser) {
      return res.status(404).json({ success: false, message: "Không tìm thấy User" });
    }

    // 4. Trả kết quả về cho Kiên
    return res.status(200).json({
      success: true,
      message: "Cập nhật thành công!",
      user: updatedUser
    });

  } catch (error) {
    console.error(error);
    return res.status(500).json({ success: false, message: "Lỗi Server" });
  }
};