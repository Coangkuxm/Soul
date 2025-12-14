const jwt = require('jsonwebtoken');
const { query, getClient } = require('../config/db-connection');
const { 
  UnauthorizedError, 
  BadRequestError, 
  ConflictError,
  ValidationError,
  DatabaseError
} = require('../utils/errors');
const bcrypt = require('bcryptjs');
const rateLimit = require('express-rate-limit');
const { body, validationResult } = require('express-validator');
const crypto = require('crypto');

// Rate limiting for auth endpoints
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 10, // limit each IP to 10 requests per windowMs
  message: 'Quá nhiều yêu cầu từ địa chỉ IP này, vui lòng thử lại sau 15 phút',
  standardHeaders: true,
  legacyHeaders: false,
  skip: (req) => {
    // Bỏ qua rate limiting cho các IP tin cậy
    const trustedIps = process.env.TRUSTED_IPS ? 
      process.env.TRUSTED_IPS.split(',') : [];
    return trustedIps.includes(req.ip);
  }
});

// Add random delay to prevent timing attacks
const delay = (ms) => new Promise(resolve => 
  setTimeout(resolve, ms + Math.floor(Math.random() * 100))
);

// Generate secure random token
const generateToken = (length = 32) => {
  return crypto.randomBytes(Math.ceil(length / 2))
    .toString('hex')
    .slice(0, length);
};

const authController = {
  // Input validation for login
  validateLogin: [
    body('email').isEmail().normalizeEmail(),
    body('password').isLength({ min: 6 })
  ],

  // Input validation for registration
  validateRegister: [
    body('username')
      .isLength({ min: 3, max: 30 })
      .trim()
      .escape(),
    body('email')
      .isEmail()
      .normalizeEmail(),
    body('password')
      .isLength({ min: 8 })
      .matches(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/),
    body('displayName')
      .optional()
      .trim()
      .escape(),
    body('bio')
      .optional()
      .trim()
      .escape()
  ],

  async login(req, res, next) {
    try {
      // Validate input
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        throw new ValidationError('Dữ liệu không hợp lệ', errors.array());
      }

      const { email, password } = req.body;

      // Add random delay to prevent timing attacks
      await delay(100 + Math.random() * 100);

      // 1. Find user by email with only necessary fields
      const result = await query(
        `SELECT id, username, email, password, display_name as "displayName", 
                avatar_url as "avatarUrl"
         FROM users WHERE email = $1`,
        [email]
      );
      const user = result.rows[0];

      // 2. Check if user exists
      if (!user) {
        console.log('User not found with email:', email);
        throw new UnauthorizedError('Email hoặc mật khẩu không đúng');
      }

      // Debug logging
      console.log('User found:', { id: user.id, email: user.email });
      console.log('Provided password:', password);
      console.log('Stored password hash:', user.password);

      // 3. Verify password with bcrypt
let isMatch = false;
try {
  isMatch = await bcrypt.compare(password, user.password);
  console.log('BCrypt compare result:', isMatch);
  
  // Nếu không khớp, thử so sánh trực tiếp (cho trường hợp mật khẩu chưa được hash)
  if (!isMatch && user.password === password) {
    console.log('Direct password match - updating to hashed password');
    const hashedPassword = await bcrypt.hash(password, 12);
    await query('UPDATE users SET password = $1 WHERE id = $2', [hashedPassword, user.id]);
    isMatch = true;
  }
} catch (err) {
  console.error('Error comparing passwords:', err);
  // Nếu có lỗi khi so sánh, thử so sánh trực tiếp
  isMatch = user.password === password;
  if (isMatch) {
    console.log('Direct comparison worked - fixing password hash');
    const hashedPassword = await bcrypt.hash(password, 12);
    await query('UPDATE users SET password = $1 WHERE id = $2', [hashedPassword, user.id]);
  }
}

if (!isMatch) {
  throw new UnauthorizedError('Email hoặc mật khẩu không đúng');
}

      // 4. Generate JWT with secure settings
      const token = jwt.sign(
        { 
          userId: user.id, 
          username: user.username,
          email: user.email,
          role: 'user',
          iat: Math.floor(Date.now() / 1000) // Issued at time
        },
        process.env.JWT_SECRET,
        { 
          expiresIn: '1d',
          algorithm: 'HS256',
          issuer: 'soul-api',
          audience: 'soul-app'
        }
      );

      // 5. Set secure httpOnly cookie
      res.cookie('token', token, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
        maxAge: 24 * 60 * 60 * 1000, // 1 day
        path: '/',
        domain: process.env.COOKIE_DOMAIN || undefined
      });

      // 6. Don't send password back
      delete user.password;
      delete user.isActive;
      
      // 7. Return user info and token (for clients that need it)
      res.json({
        success: true,
        message: 'Đăng nhập thành công',
        user,
        token // Still send token for mobile clients if needed
      });
    } catch (error) {
      next(error);
    }
  },

  async register(req, res, next) {
    const client = await require('../config/db-connection').pool.connect();
    
    try {
      // Validate input
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        return next(new ValidationError('Dữ liệu không hợp lệ', errors.array()));
      }

      const { username, email, password, displayName, avatarUrl, bio } = req.body;

      try {
        await client.query('BEGIN');

        // 1. Check if email exists (case-insensitive)
        const emailExists = await client.query(
          'SELECT id FROM users WHERE LOWER(email) = LOWER($1) FOR UPDATE',
          [email]
        );
        
        if (emailExists.rows.length > 0) {
          throw new ConflictError('Email đã được sử dụng');
        }

        // 2. Hash password with bcrypt
        console.log('Original password:', password);
        const hashedPassword = await bcrypt.hash(password, 12);
        console.log('Hashed password:', hashedPassword);

        // 3. Insert new user into database
        const result = await client.query(
          `INSERT INTO users (username, email, password, display_name, avatar_url, bio)
           VALUES ($1, $2, $3, $4, $5, $6) 
           RETURNING id, username, email, display_name as "displayName", 
                     avatar_url as "avatarUrl", bio, created_at as "createdAt"`,
          [username, email, hashedPassword, displayName, avatarUrl, bio]
        );
        
        // 4. Commit transaction
        await client.query('COMMIT');
        
        const user = result.rows[0];
        console.log('User created successfully:', user.id);

        // 5. Generate JWT with secure settings
        const token = jwt.sign(
          { 
            userId: user.id, 
            username: user.username,
            email: user.email,
            role: 'user',
            iat: Math.floor(Date.now() / 1000) // Issued at time
          },
          process.env.JWT_SECRET || 'your-secret-key',
          { 
            expiresIn: '1d',
            algorithm: 'HS256',
            issuer: process.env.JWT_ISSUER || 'soul-api',
            audience: process.env.JWT_AUDIENCE || 'soul-app'
          }
        );

        // 6. Set secure httpOnly cookie
        res.cookie('token', token, {
          httpOnly: true,
          secure: process.env.NODE_ENV === 'production',
          sameSite: 'strict',
          maxAge: 24 * 60 * 60 * 1000, // 1 day
          path: '/',
          domain: process.env.COOKIE_DOMAIN || undefined
        });

        // 7. Return success response
        res.status(201).json({
          success: true,
          message: 'Đăng ký thành công',
          user,
          token
        });
      } catch (error) {
        console.error('Registration error:', error);
        await client.query('ROLLBACK');
        next(error);
      } finally {
        client.release();
      }
    } catch (error) {
      next(error);
    }
  },

  // Logout handler
  async logout(req, res, next) {
    try {
      // Clear the token cookie
      res.clearCookie('token', {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
        path: '/',
        domain: process.env.COOKIE_DOMAIN || undefined
      });
      
      // Nếu có refresh token, thêm vào danh sách đen
      if (req.cookies?.refreshToken) {
        const token = req.cookies.refreshToken;
        const expiresAt = new Date();
        expiresAt.setDate(expiresAt.getDate() + 7); // Hết hạn sau 7 ngày
        
        await query(
          'INSERT INTO revoked_tokens (token, expires_at) VALUES ($1, $2)',
          [token, expiresAt]
        );
        
        // Xóa refresh token cookie
        res.clearCookie('refreshToken', {
          httpOnly: true,
          secure: process.env.NODE_ENV === 'production',
          sameSite: 'strict',
          path: '/auth/refresh-token',
          domain: process.env.COOKIE_DOMAIN || undefined
        });
      }
      
      res.json({ success: true, message: 'Đăng xuất thành công' });
    } catch (error) {
      next(new DatabaseError('Không thể đăng xuất ngay lúc này'));
    }
  },

  // Refresh token handler
  async refreshToken(req, res, next) {
    try {
      const refreshToken = req.cookies?.refreshToken;
      
      if (!refreshToken) {
        throw new UnauthorizedError('Không tìm thấy refresh token');
      }
      
      // Kiểm tra xem token có trong danh sách đen không
      const revoked = await query(
        'SELECT id FROM revoked_tokens WHERE token = $1 AND expires_at > NOW()',
        [refreshToken]
      );
      
      if (revoked.rows.length > 0) {
        throw new UnauthorizedError('Token không hợp lệ');
      }
      
      // Xác thực refresh token
      const decoded = jwt.verify(refreshToken, process.env.JWT_REFRESH_SECRET, {
        issuer: process.env.JWT_ISSUER,
        audience: process.env.JWT_AUDIENCE
      });
      
      // Tạo access token mới
      const accessToken = jwt.sign(
        {
          userId: decoded.userId,
          username: decoded.username,
          email: decoded.email,
          role: decoded.role,
          iat: Math.floor(Date.now() / 1000)
        },
        process.env.JWT_SECRET,
        { 
          expiresIn: process.env.JWT_EXPIRES_IN || '15m',
          algorithm: 'HS256',
          issuer: process.env.JWT_ISSUER,
          audience: process.env.JWT_AUDIENCE
        }
      );
      
      // Đặt lại thời gian sống cho refresh token nếu gần hết hạn
      const now = new Date();
      const expires = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 ngày
      
      // Cập nhật cookie với access token mới
      res.cookie('token', accessToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
        maxAge: 15 * 60 * 1000, // 15 phút
        path: '/',
        domain: process.env.COOKIE_DOMAIN || undefined
      });
      
      res.json({
        success: true,
        token: accessToken,
        expiresIn: 15 * 60 // 15 phút (tính bằng giây)
      });
      
    } catch (error) {
      if (error.name === 'TokenExpiredError') {
        return next(new UnauthorizedError('Refresh token đã hết hạn'));
      }
      if (error.name === 'JsonWebTokenError') {
        return next(new UnauthorizedError('Refresh token không hợp lệ'));
      }
      next(error);
    }
  },

  // Rate limiter middleware for auth endpoints
  authLimiter,
  
  // Helper function to set auth cookies
  setAuthCookies(res, token, refreshToken) {
    // Set access token cookie (15 phút)
    res.cookie('token', token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 15 * 60 * 1000, // 15 phút
      path: '/',
      domain: process.env.COOKIE_DOMAIN || undefined
    });
    
    // Set refresh token cookie (7 ngày)
    if (refreshToken) {
      res.cookie('refreshToken', refreshToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict',
        maxAge: 7 * 24 * 60 * 60 * 1000, // 7 ngày
        path: '/auth/refresh-token',
        domain: process.env.COOKIE_DOMAIN || undefined
      });
    }
  }
};

module.exports = authController;