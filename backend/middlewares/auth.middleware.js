const jwt = require('jsonwebtoken');
const { UnauthorizedError, ForbiddenError } = require('../utils/errors');

const authenticateToken = (req, res, next) => {
  // Lấy token từ header Authorization hoặc cookie
  const authHeader = req.headers['authorization'];
  const cookieToken = req.cookies && req.cookies.token;
  const token = (authHeader && authHeader.split(' ')[1]) || cookieToken;

  if (!token) {
    return next(new UnauthorizedError('Vui lòng đăng nhập để tiếp tục'));
  }

  try {
    // Xác thực token
    const decoded = jwt.verify(token, process.env.JWT_SECRET, {
      algorithms: ['HS256'],
      issuer: 'soul-api',
      audience: 'soul-app'
    });
    
    // Gắn thông tin user vào request để sử dụng trong các route
    req.user = {
      id: decoded.userId,
      username: decoded.username,
      role: decoded.role || 'user' // Mặc định là 'user' nếu không có role
    };
    
    next();
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return next(new UnauthorizedError('Phiên đăng nhập đã hết hạn'));
    }
    next(new UnauthorizedError('Token không hợp lệ'));
  }
};

// Middleware kiểm tra quyền admin
const isAdmin = (req, res, next) => {
  if (req.user && req.user.role === 'admin') {
    return next();
  }
  next(new ForbiddenError('Không có quyền truy cập'));
};

module.exports = {
  authenticateToken,
  isAdmin
};
