
const { body, validationResult } = require('express-validator');
const { ValidationError } = require('../utils/errors');
// Định nghĩa các rules validation
const validationRules = {
  register: [
    body('username')
      .trim()
      .isLength({ min: 3, max: 20 })
      .withMessage('Tên đăng nhập phải từ 3-20 ký tự'),
    body('email')
      .isEmail()
      .normalizeEmail()
      .withMessage('Email không hợp lệ'),
    body('password')
      .isLength({ min: 6 })
      .withMessage('Mật khẩu phải có ít nhất 6 ký tự')
  ],
  login: [
    body('email')
      .isEmail()
      .normalizeEmail()
      .withMessage('Email không hợp lệ'),
    body('password')
      .notEmpty()
      .withMessage('Vui lòng nhập mật khẩu')
  ],
  updateProfile: [
    body('displayName')
      .optional()
      .trim()
      .isLength({ min: 2, max: 50 })
      .withMessage('Tên hiển thị phải từ 2-50 ký tự'),
    body('avatarUrl')
      .optional()
      .isURL()
      .withMessage('URL avatar không hợp lệ')
  ],
  changePassword: [
    body('currentPassword')
      .notEmpty()
      .withMessage('Vui lòng nhập mật khẩu hiện tại'),
    body('newPassword')
      .isLength({ min: 6 })
      .withMessage('Mật khẩu mới phải có ít nhất 6 ký tự')
  ]
};

// Middleware xử lý validation
const validate = (rule) => {
  return [
    ...(validationRules[rule] || []),
    (req, res, next) => {
      const errors = validationResult(req);
      if (errors.isEmpty()) {
        return next();
      }
      throw new ValidationError('Dữ liệu không hợp lệ', errors.array());
    }
  ];
};

module.exports = { validate };
