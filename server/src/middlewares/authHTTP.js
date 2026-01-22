const jwt = require('jsonwebtoken');

module.exports = (req, res, next) => {
    let token = req.header('Authorization');

    if (!token) {
        console.log("Auth: Thiếu header Authorization");
        return res.status(401).json({ message: "Không có quyền truy cập!" });
    }

    try {
        // Xử lý chuỗi Bearer một cách an toàn hơn
        if (token.startsWith('Bearer ')) {
            token = token.slice(7, token.length).trim();
        }

        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded;
        next();
    } catch (err) {
        console.error("Auth: Token không hợp lệ!", err.message);
        // Trả về 401 (Unauthorized) thay vì 400 để đúng chuẩn hơn
        res.status(401).json({ message: "Phiên đăng nhập hết hạn!" });
    }
};