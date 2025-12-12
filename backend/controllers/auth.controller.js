const jwt = require('jsonwebtoken');
const { query } = require('../config/db-connection');
const { UnauthorizedError } = require('../utils/errors');

const authController = {
  async login(req, res, next) {
    try {
      const { email, password } = req.body;

      // 1. Tìm user bằng email
      const result = await query(
        'SELECT * FROM users WHERE email = $1',
        [email]
      );
      const user = result.rows[0];

      // 2. Kiểm tra user có tồn tại không
      if (!user) {
        throw new UnauthorizedError('Email hoặc mật khẩu không đúng');
      }

      // 3. Xác thực mật khẩu (so sánh trực tiếp vì dùng mật khẩu thường)
      if (user.password !== password) {
        throw new UnauthorizedError('Email hoặc mật khẩu không đúng');
      }

      // 4. Tạo JWT token
      const token = jwt.sign(
        { userId: user.id, username: user.username },
        process.env.JWT_SECRET,
        { expiresIn: '1d' }
      );

      // 5. Trả về thông tin user và token
      res.json({
        success: true,
        token,
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          displayName: user.display_name,
          avatarUrl: user.avatar_url
        }
      });

    } catch (error) {
      next(error);
    }
  }
};

module.exports = authController;