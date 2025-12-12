const { body, param, query } = require('express-validator');

const userValidation = {
  // Validation cho đăng ký
  register: [
    body('username')
      .trim()
      .notEmpty().withMessage('Tên đăng nhập là bắt buộc')
      .isLength({ min: 3, max: 30 }).withMessage('Tên đăng nhập phải từ 3 đến 30 ký tự')
      .matches(/^[a-zA-Z0-9_]+$/).withMessage('Tên đăng nhập chỉ được chứa chữ cái, số và dấu gạch dưới'),
      
    body('email')
      .trim()
      .notEmpty().withMessage('Email là bắt buộc')
      .isEmail().withMessage('Email không hợp lệ')
      .normalizeEmail(),
      
    body('password')
      .notEmpty().withMessage('Mật khẩu là bắt buộc')
      .isLength({ min: 6 }).withMessage('Mật khẩu phải có ít nhất 6 ký tự'),
      
    body('displayName')
      .optional()
      .trim()
      .isLength({ max: 100 }).withMessage('Tên hiển thị không quá 100 ký tự'),
  ],

  // Validation cho đăng nhập
  login: [
    body('email')
      .trim()
      .notEmpty().withMessage('Email là bắt buộc')
      .isEmail().withMessage('Email không hợp lệ'),
      
    body('password')
      .notEmpty().withMessage('Mật khẩu là bắt buộc'),
  ],

  // Validation cho cập nhật thông tin
  updateProfile: [
    body('displayName')
      .optional()
      .trim()
      .isLength({ max: 100 }).withMessage('Tên hiển thị không quá 100 ký tự'),
      
    body('bio')
      .optional()
      .trim()
      .isLength({ max: 500 }).withMessage('Tiểu sử không quá 500 ký tự'),
      
    body('avatarUrl')
      .optional()
      .isURL().withMessage('URL ảnh đại diện không hợp lệ'),
  ],

  // Validation cho đổi mật khẩu
  changePassword: [
    body('currentPassword')
      .notEmpty().withMessage('Mật khẩu hiện tại là bắt buộc'),
      
    body('newPassword')
      .notEmpty().withMessage('Mật khẩu mới là bắt buộc')
      .isLength({ min: 6 }).withMessage('Mật khẩu mới phải có ít nhất 6 ký tự')
      .custom((value, { req }) => {
        if (value === req.body.currentPassword) {
          throw new Error('Mật khẩu mới phải khác mật khẩu hiện tại');
        }
        return true;
      }),
  ],

  // Validation cho ID
  userIdParam: [
    param('id')
      .isInt({ min: 1 }).withMessage('ID người dùng không hợp lệ')
      .toInt(),
  ],

  // Validation cho phân trang và tìm kiếm
  listUsers: [
    query('page')
      .optional()
      .isInt({ min: 1 }).withMessage('Số trang phải là số nguyên dương')
      .toInt(),
      
    query('limit')
      .optional()
      .isInt({ min: 1, max: 100 }).withMessage('Số lượng mỗi trang phải từ 1 đến 100')
      .toInt(),
      
    query('search')
      .optional()
      .trim()
      .isLength({ max: 100 }).withMessage('Từ khóa tìm kiếm không quá 100 ký tự'),
  ],
};

module.exports = userValidation;
