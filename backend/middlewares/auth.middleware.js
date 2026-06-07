const jwt = require('jsonwebtoken');
const { query } = require('../config/db-connection');
const { UnauthorizedError, ForbiddenError } = require('../utils/errors');

const authenticateToken = async (req, res, next) => {
  const authHeader = req.headers.authorization;
  const cookieToken = req.cookies && req.cookies.token;
  const token = (authHeader && authHeader.split(' ')[1]) || cookieToken;

  if (!token) {
    return next(new UnauthorizedError('Vui lòng đăng nhập để tiếp tục'));
  }

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET, {
      algorithms: ['HS256'],
      issuer: 'soul-api',
      audience: 'soul-app'
    });

    const userResult = await query(
      `SELECT id, username, role, account_status
       FROM users
       WHERE id = $1`,
      [decoded.userId]
    );

    if (userResult.rows.length === 0) {
      return next(new UnauthorizedError('Tài khoản không còn tồn tại'));
    }

    const dbUser = userResult.rows[0];
    if ((dbUser.account_status || 'active') !== 'active') {
      return next(new ForbiddenError('Tài khoản đã bị khóa'));
    }

    req.user = {
      id: dbUser.id,
      username: dbUser.username || decoded.username,
      role: dbUser.role || decoded.role || 'user'
    };

    next();
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return next(new UnauthorizedError('Phiên đăng nhập đã hết hạn'));
    }
    next(new UnauthorizedError('Token không hợp lệ'));
  }
};

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
