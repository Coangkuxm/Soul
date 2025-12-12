const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const userModel = require('../models/user.model');
const { 
  NotFoundError, 
  ConflictError, 
  UnauthorizedError,
  BadRequestError 
} = require('../utils/errors');

const userController = {
  // Lấy thông tin người dùng hiện tại
  async getProfile(req, res, next) {
    try {
      // Lấy thông tin user từ token (đã được xác thực bởi middleware)
      const user = await userModel.findById(req.user.userId);
      
      if (!user) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }

      // Ẩn thông tin nhạy cảm
      const { password, ...userWithoutPassword } = user;

      res.json({
        success: true,
        user: userWithoutPassword
      });
    } catch (error) {
      next(error);
    }
  },

  // Đăng ký người dùng mới
  async register(req, res, next) {
    try {
      const { username, email, password, displayName, avatarUrl, bio } = req.body;
      
      // Kiểm tra email đã tồn tại chưa
      const existingUser = await userModel.findByEmail(email);
      if (existingUser) {
        throw new ConflictError('Email đã được sử dụng');
      }
      
      // Tạo người dùng mới
      const newUser = await userModel.create({
        username,
        email,
        password, // Lưu ý: Nên mã hóa mật khẩu ở tầng service
        displayName,
        avatarUrl,
        bio
      });
      
      // Tạo token
      const token = jwt.sign(
        { userId: newUser.id, username: newUser.username, role: 'user' },
        process.env.JWT_SECRET,
        { expiresIn: '1d' }
      );
      
      res.status(201).json({
        success: true,
        token,
        user: newUser
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Lấy thông tin profile người dùng hiện tại
  async getProfile(req, res, next) {
    try {
      const user = await userModel.findById(req.user.id);
      if (!user) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }
      
      res.json({
        success: true,
        user
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Cập nhật thông tin người dùng
  async updateProfile(req, res, next) {
    try {
      const { displayName, avatarUrl, bio } = req.body;
      
      const updatedUser = await userModel.update(req.user.id, {
        displayName,
        avatarUrl,
        bio
      });
      
      res.json({
        success: true,
        user: updatedUser
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Đổi mật khẩu
  async changePassword(req, res, next) {
    try {
      const { currentPassword, newPassword } = req.body;
      
      // Lấy thông tin người dùng
      const user = await userModel.findById(req.user.id);
      if (!user) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }
      
      // Kiểm tra mật khẩu hiện tại
      if (user.password !== currentPassword) {
        throw new UnauthorizedError('Mật khẩu hiện tại không đúng');
      }
      
      // Cập nhật mật khẩu mới
      await userModel.changePassword(user.id, newPassword);
      
      res.json({
        success: true,
        message: 'Đổi mật khẩu thành công'
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Lấy danh sách người dùng (phân trang)
  async getAllUsers(req, res, next) {
    try {
      const { page = 1, limit = 10, search = '' } = req.query;
      
      const users = await userModel.getAll({ 
        page: parseInt(page), 
        limit: parseInt(limit), 
        search 
      });
      
      // Lấy tổng số người dùng để phân trang
      const total = await userModel.countUsers(search);
      const totalPages = Math.ceil(total / limit);
      
      res.json({
        success: true,
        data: users,
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total,
          totalPages
        }
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Lấy thông tin người dùng theo ID
  async getUserById(req, res, next) {
    try {
      const user = await userModel.findById(req.params.id);
      if (!user) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }
      
      res.json({
        success: true,
        user
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Cập nhật thông tin người dùng (admin)
  async updateUser(req, res, next) {
    try {
      const { id } = req.params;
      const { username, email, displayName, avatarUrl, bio } = req.body;
      
      // Kiểm tra email đã tồn tại chưa (nếu có thay đổi email)
      if (email) {
        const existingUser = await userModel.findByEmail(email);
        if (existingUser && existingUser.id !== parseInt(id)) {
          throw new ConflictError('Email đã được sử dụng bởi người dùng khác');
        }
      }
      
      // Cập nhật thông tin
      const updatedUser = await userModel.update(id, {
        username,
        email,
        displayName,
        avatarUrl,
        bio
      });
      
      res.json({
        success: true,
        user: updatedUser
      });
      
    } catch (error) {
      next(error);
    }
  },

  // Xóa người dùng (admin)
  async deleteUser(req, res, next) {
    try {
      const { id } = req.params;
      
      // Kiểm tra không được xóa chính mình
      if (parseInt(id) === req.user.id) {
        throw new BadRequestError('Không thể xóa tài khoản của chính bạn');
      }
      
      // Xóa người dùng
      await userModel.delete(id);
      
      res.json({
        success: true,
        message: 'Đã xóa người dùng thành công'
      });
      
    } catch (error) {
      next(error);
    }
  },
  
  // Theo dõi người dùng
  async followUser(req, res, next) {
    try {
      const { id } = req.params;
      
      // Không được tự theo dõi chính mình
      if (parseInt(id) === req.user.id) {
        throw new BadRequestError('Không thể tự theo dõi chính mình');
      }
      
      // Kiểm tra người dùng tồn tại
      const userToFollow = await userModel.findById(id);
      if (!userToFollow) {
        throw new NotFoundError('Không tìm thấy người dùng');
      }
      
      // Thực hiện theo dõi
      await userModel.followUser(req.user.id, id);
      
      res.json({
        success: true,
        message: 'Đã theo dõi người dùng',
        isFollowing: true
      });
      
    } catch (error) {
      next(error);
    }
  },
  
  // Bỏ theo dõi người dùng
  async unfollowUser(req, res, next) {
    try {
      const { id } = req.params;
      
      // Bỏ theo dõi
      await userModel.unfollowUser(req.user.id, id);
      
      res.json({
        success: true,
        message: 'Đã bỏ theo dõi người dùng',
        isFollowing: false
      });
      
    } catch (error) {
      next(error);
    }
  },
  
  // Lấy danh sách người theo dõi
  async getFollowers(req, res, next) {
    try {
      const { id } = req.params;
      const followers = await userModel.getFollowers(id);
      
      res.json({
        success: true,
        data: followers
      });
      
    } catch (error) {
      next(error);
    }
  },
  
  // Lấy danh sách đang theo dõi
  async getFollowing(req, res, next) {
    try {
      const { id } = req.params;
      const following = await userModel.getFollowing(id);
      
      res.json({
        success: true,
        data: following
      });
      
    } catch (error) {
      next(error);
    }
  },
  
  // Kiểm tra đang theo dõi
  async checkFollowing(req, res, next) {
    try {
      const { id } = req.params;
      const isFollowing = await userModel.isFollowing(req.user.id, id);
      
      res.json({
        success: true,
        isFollowing
      });
      
    } catch (error) {
      next(error);
    }
  }
};

module.exports = userController;