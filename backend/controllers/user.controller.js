const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const { query } = require('../db-connection');
const { NotFoundError, ConflictError, UnauthorizedError } = require('../utils/errors');

const userController = {
  // Đăng ký người dùng mới
  async register(req, res, next) {
    try {
      const { username, email, password } = req.body;
      
      // Kiểm tra email đã tồn tại chưa
      const existingUser = await query(
        'SELECT id FROM users WHERE email = $1',
        [email]
      );
      
      if (existingUser.rows.length > 0) {
        throw new ConflictError('Email đã được sử dụng');
      }
      
      // Mã hóa mật khẩu
      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash(password, salt);
      
      // Tạo người dùng mới
      const result = await query(
        `INSERT INTO users (username, email, password_hash, created_at, updated_at)
         VALUES ($1, $2, $3, NOW(), NOW())
         RETURNING id, username, email, display_name, avatar_url, created_at`,
        [username, email, hashedPassword]
      );
      
      const newUser = result.rows[0];
      
      // Tạo token
      const token = jwt.sign(
        { userId: newUser.id, username: newUser.username, role: 'user' },
        process.env.JWT_SECRET,
        { expiresIn: '1d' }
      );
      
      res.status(201).json({
        success: true,
        token,
        user: {
          id: newUser.id,
          username: newUser.username,
          email: newUser.email,
          displayName: newUser.display_name,
          avatarUrl: newUser.avatar_url,
          createdAt: newUser.created_at
        }
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Lấy thông tin profile người dùng hiện tại
  async getProfile(req, res, next) {
    try {
      const result = await query(
        'SELECT id, username, email, display_name, avatar_url, created_at FROM users WHERE id = $1',
        [req.user.id]
      );
      
      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }
      
      const user = result.rows[0];
      
      res.json({
        success: true,
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          displayName: user.display_name,
          avatarUrl: user.avatar_url,
          createdAt: user.created_at
        }
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Cập nhật thông tin người dùng
  async updateProfile(req, res, next) {
    try {
      const { displayName, avatarUrl } = req.body;
      
      const result = await query(
        `UPDATE users 
         SET display_name = COALESCE($1, display_name),
             avatar_url = COALESCE($2, avatar_url),
             updated_at = NOW()
         WHERE id = $3
         RETURNING id, username, email, display_name, avatar_url, created_at`,
        [displayName, avatarUrl, req.user.id]
      );
      
      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }
      
      const user = result.rows[0];
      
      res.json({
        success: true,
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          displayName: user.display_name,
          avatarUrl: user.avatar_url,
          createdAt: user.created_at
        }
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Đổi mật khẩu
  async changePassword(req, res, next) {
    try {
      const { currentPassword, newPassword } = req.body;
      
      // Lấy mật khẩu hiện tại từ database
      const result = await query(
        'SELECT password_hash FROM users WHERE id = $1',
        [req.user.id]
      );
      
      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }
      
      const user = result.rows[0];
      
      // Kiểm tra mật khẩu hiện tại
      const isMatch = await bcrypt.compare(currentPassword, user.password_hash);
      if (!isMatch) {
        throw new UnauthorizedError('Mật khẩu hiện tại không đúng');
      }
      
      // Mã hóa mật khẩu mới
      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash(newPassword, salt);
      
      // Cập nhật mật khẩu mới
      await query(
        'UPDATE users SET password_hash = $1, updated_at = NOW() WHERE id = $2',
        [hashedPassword, req.user.id]
      );
      
      res.json({
        success: true,
        message: 'Đổi mật khẩu thành công'
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Lấy tất cả người dùng (chỉ admin)
  async getAllUsers(req, res, next) {
    try {
      const result = await query(
        'SELECT id, username, email, display_name, avatar_url, created_at FROM users ORDER BY created_at DESC'
      );
      
      const users = result.rows.map(user => ({
        id: user.id,
        username: user.username,
        email: user.email,
        displayName: user.display_name,
        avatarUrl: user.avatar_url,
        createdAt: user.created_at
      }));
      
      res.json({
        success: true,
        count: users.length,
        users
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Lấy thông tin người dùng theo ID (chỉ admin)
  async getUserById(req, res, next) {
    try {
      const { id } = req.params;
      
      const result = await query(
        'SELECT id, username, email, display_name, avatar_url, created_at FROM users WHERE id = $1',
        [id]
      );
      
      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }
      
      const user = result.rows[0];
      
      res.json({
        success: true,
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          displayName: user.display_name,
          avatarUrl: user.avatar_url,
          createdAt: user.created_at
        }
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Cập nhật thông tin người dùng (chỉ admin)
  async updateUser(req, res, next) {
    try {
      const { id } = req.params;
      const { username, email, displayName, role } = req.body;
      
      const result = await query(
        `UPDATE users 
         SET username = COALESCE($1, username),
             email = COALESCE($2, email),
             display_name = COALESCE($3, display_name),
             role = COALESCE($4, role),
             updated_at = NOW()
         WHERE id = $5
         RETURNING id, username, email, display_name, avatar_url, role, created_at`,
        [username, email, displayName, role, id]
      );
      
      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }
      
      const user = result.rows[0];
      
      res.json({
        success: true,
        user: {
          id: user.id,
          username: user.username,
          email: user.email,
          displayName: user.display_name,
          avatarUrl: user.avatar_url,
          role: user.role,
          createdAt: user.created_at
        }
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Xóa người dùng (chỉ admin)
  async deleteUser(req, res, next) {
    try {
      const { id } = req.params;
      
      // Không cho xóa chính mình
      if (id === req.user.id) {
        throw new ForbiddenError('Không thể xóa tài khoản của chính bạn');
      }
      
      const result = await query(
        'DELETE FROM users WHERE id = $1 RETURNING id',
        [id]
      );
      
      if (result.rowCount === 0) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }
      
      res.json({
        success: true,
        message: 'Đã xóa người dùng thành công'
      });
      
    } catch (error) {
      next(error);
    }
  }
};

module.exports = userController;