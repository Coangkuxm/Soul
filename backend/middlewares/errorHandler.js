const { ValidationError } = require('express-json-validator-middleware');
const { UnauthorizedError, NotFoundError } = require('../utils/errors');

const errorHandler = (err, req, res, next) => {
  console.error('Error:', err);
  
  // Handle JWT errors
  if (err.name === 'JsonWebTokenError') {
    return res.status(401).json({
      success: false,
      error: 'Token không hợp lệ'
    });
  }

  // Handle JWT expired
  if (err.name === 'TokenExpiredError') {
    return res.status(401).json({
      success: false,
      error: 'Token đã hết hạn'
    });
  }

  // Handle validation errors
  if (err instanceof ValidationError) {
    return res.status(400).json({
      success: false,
      error: 'Dữ liệu không hợp lệ',
      details: err.validationErrors
    });
  }

  // Handle custom errors
  if (err instanceof UnauthorizedError) {
    return res.status(401).json({
      success: false,
      error: err.message
    });
  }

  if (err instanceof NotFoundError) {
    return res.status(404).json({
      success: false,
      error: err.message
    });
  }

  // Default error handler
  res.status(500).json({
    success: false,
    error: 'Đã xảy ra lỗi máy chủ',
    message: process.env.NODE_ENV === 'development' ? err.message : undefined
  });
};

module.exports = {
  errorHandler
};
