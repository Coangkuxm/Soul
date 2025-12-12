const express = require('express');
const router = express.Router();
const userController = require('../controllers/user.controller');
const { authenticateToken } = require('../middlewares/auth.middleware');
const { validate } = require('../middlewares/validation.middleware');
const userValidation = require('../validations/user.validations');

// ==================== PUBLIC ROUTES ====================

// Đăng ký tài khoản
router.post(
  '/register',
  userValidation.register,
  validate('register'),
  userController.register
);

// ==================== PROTECTED ROUTES ====================
router.use(authenticateToken);

// Quản lý profile
router.get('/me', userController.getProfile);
router.put(
  '/me',
  userValidation.updateProfile,
  validate('updateProfile'),
  userController.updateProfile
);
router.put(
  '/change-password',
  userValidation.changePassword,
  validate('changePassword'),
  userController.changePassword
);

// Theo dõi người dùng
router.post('/:id/follow', userController.followUser);
router.delete('/:id/unfollow', userController.unfollowUser);
router.get('/:id/followers', userController.getFollowers);
router.get('/:id/following', userController.getFollowing);
router.get('/:id/is-following', userController.checkFollowing);

// Xem thông tin người dùng khác (chỉ chấp nhận số làm ID)
router.get('/:id', userController.getUserById);

// ==================== ADMIN ROUTES ====================

// Lấy danh sách người dùng (phân trang, tìm kiếm)
router.get(
  '/',
  userValidation.listUsers,
  validate('listUsers'),
  userController.getAllUsers
);

// Cập nhật thông tin người dùng (admin)
router.put(
  '/:id',
  userValidation.updateProfile,
  validate('updateUser'),
  userController.updateUser
);

// Xóa người dùng (admin)
router.delete('/:id', userController.deleteUser);

module.exports = router;