const jwt = require('jsonwebtoken');
const { UnauthorizedError, ForbiddenError } = require('../utils/errors');

// Middleware xác thực JWT
exports.authenticateJWT = (req, res, next) => {
  try {
    // Lấy token từ cookie hoặc header Authorization
    let token = req.cookies?.token || 
               (req.headers.authorization && req.headers.authorization.split(' ')[1]);

    if (!token) {
      throw new UnauthorizedError('Vui lòng đăng nhập để tiếp tục');
    }

    // Xác thực token
    const decoded = jwt.verify(token, process.env.JWT_SECRET, {
      algorithms: ['HS256'],
      issuer: process.env.JWT_ISSUER,
      audience: process.env.JWT_AUDIENCE
    });

    // Lưu thông tin người dùng vào request
    req.user = {
      id: decoded.userId,
      username: decoded.username,
      email: decoded.email,
      role: decoded.role || 'user'
    };

    next();
  } catch (error) {
    // Xử lý các loại lỗi JWT
    if (error.name === 'TokenExpiredError') {
      return next(new UnauthorizedError('Phiên đăng nhập đã hết hạn'));
    }
    if (error.name === 'JsonWebTokenError') {
      return next(new UnauthorizedError('Token không hợp lệ'));
    }
    next(error);
  }
};

// Middleware kiểm tra vai trò người dùng
exports.authorizeRoles = (...roles) => {
  return (req, res, next) => {
    if (!roles.includes(req.user.role)) {
      return next(new ForbiddenError('Không có quyền truy cập tài nguyên này'));
    }
    next();
  };
};

// Middleware kiểm tra quyền sở hữu
exports.checkOwnership = (modelName, idParam = 'id') => {
  return async (req, res, next) => {
    try {
      const model = require(`../models/${modelName}.model`);
      const item = await model.getById(req.params[idParam]);
      
      if (!item) {
        return next(new NotFoundError(`${modelName} không tồn tại`));
      }

      // Cho phép admin hoặc chính chủ
      if (req.user.role !== 'admin' && item.userId !== req.user.id) {
        return next(new ForbiddenError('Bạn không có quyền thực hiện hành động này'));
      }

      // Lưu đối tượng vào request để sử dụng ở các middleware tiếp theo
      req[modelName] = item;
      next();
    } catch (error) {
      next(error);
    }
  };
};

// Middleware chống tấn công brute force
exports.preventBruteForce = (req, res, next) => {
  // Implement rate limiting logic here
  // Có thể sử dụng express-rate-limit hoặc Redis để lưu trữ số lần thử
  next();
};

// Middleware bảo vệ CSRF
exports.csrfProtection = (req, res, next) => {
  // Implement CSRF protection logic
  // Có thể sử dụng csurf middleware hoặc tự triển khai
  next();
};

// Middleware xử lý CORS an toàn
const cors = require('cors');
const allowedOrigins = process.env.CORS_ORIGIN ? 
  process.env.CORS_ORIGIN.split(',') : [];

exports.corsOptions = cors({
  origin: (origin, callback) => {
    // Cho phép request không có origin (như mobile app, postman)
    if (!origin) return callback(null, true);
    
    if (allowedOrigins.indexOf(origin) === -1) {
      const msg = 'The CORS policy for this site does not allow access from the specified Origin.';
      return callback(new Error(msg), false);
    }
    return callback(null, true);
  },
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  exposedHeaders: ['Content-Range', 'X-Content-Range']
});
