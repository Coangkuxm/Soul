const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { RateLimiterMemory } = require('rate-limiter-flexible');
const { TooManyRequestsError } = require('../utils/errors');

// Cấu hình các security headers cơ bản
const securityHeaders = (app) => {
  // Sử dụng helmet với các tùy chọn mặc định
  app.use(helmet());
  
  // Thêm các security headers bổ sung
  app.use((req, res, next) => {
    // Chống clickjacking
    res.setHeader('X-Frame-Options', 'DENY');
    
    // Chống XSS
    res.setHeader('X-XSS-Protection', '1; mode=block');
    
    // Chống MIME type sniffing
    res.setHeader('X-Content-Type-Options', 'nosniff');
    
    // Chống tham chiếu nguồn không an toàn
    const csp = [
      "default-src 'self'",
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
      "style-src 'self' 'unsafe-inline'",
      "img-src 'self' data: https: http:",
      "font-src 'self'",
      "connect-src 'self'",
      `form-action 'self'`,
      `frame-ancestors 'none'`,
      `base-uri 'self'`,
      `object-src 'none'`
    ];
    
    res.setHeader('Content-Security-Policy', csp.join('; '));
    
    // HSTS - Chỉ bật trong môi trường production
    if (process.env.NODE_ENV === 'production') {
      res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains; preload');
    }
    
    // Chống cache với các response nhạy cảm
    if (req.path.includes('/api/auth/')) {
      res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
      res.setHeader('Pragma', 'no-cache');
      res.setHeader('Expires', '0');
      res.setHeader('Surrogate-Control', 'no-store');
    }
    
    next();
  });
};

// Cấu hình rate limiting toàn cục
const globalRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 phút
  max: 100, // Giới hạn mỗi IP 100 request mỗi 15 phút
  message: 'Quá nhiều yêu cầu từ địa chỉ IP này, vui lòng thử lại sau 15 phút',
  standardHeaders: true,
  legacyHeaders: false,
  skip: (req) => {
    // Bỏ qua rate limiting cho các IP tin cậy
    const trustedIps = process.env.TRUSTED_IPS ? 
      process.env.TRUSTED_IPS.split(',').map(ip => ip.trim()) : [];
    return trustedIps.includes(req.ip);
  }
});

// Rate limiting cho API endpoint đăng nhập
const loginRateLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 phút
  max: 5, // Giới hạn 5 lần đăng nhập mỗi 15 phút
  message: 'Quá nhiều lần thử đăng nhập, vui lòng thử lại sau 15 phút',
  standardHeaders: true,
  legacyHeaders: false,
  keyGenerator: (req) => {
    // Sử dụng email hoặc IP nếu không có email
    return req.body.email || req.ip;
  }
});

// Rate limiting cho API endpoint đăng ký
const registerRateLimiter = rateLimit({
  windowMs: 60 * 60 * 1000, // 1 giờ
  max: 3, // Giới hạn 3 tài khoản mới mỗi giờ từ 1 IP
  message: 'Quá nhiều yêu cầu đăng ký từ địa chỉ IP này, vui lòng thử lại sau 1 giờ',
  standardHeaders: true,
  legacyHeaders: false
});

// Rate limiting linh hoạt hơn với rate-limiter-flexible
const rateLimiter = new RateLimiterMemory({
  points: 100, // Số điểm tối đa
  duration: 60, // Thời gian tính bằng giây
  blockDuration: 60 * 5, // Chặn trong 5 phút nếu vượt quá giới hạn
  inmemoryBlockOnConsumed: 150, // Tự động chặn nếu vượt quá 150 điểm
  inmemoryBlockDuration: 60 * 5 // Thời gian chặn: 5 phút
});

// Middleware rate limiting linh hoạt
const flexibleRateLimiter = (req, res, next) => {
  const key = req.user ? `user_${req.user.id}` : `ip_${req.ip}`;
  
  rateLimiter.consume(key, 1) // Trừ 1 điểm cho mỗi request
    .then(rateLimiterRes => {
      // Thêm thông tin rate limit vào header
      res.set({
        'X-RateLimit-Limit': rateLimiter.points,
        'X-RateLimit-Remaining': rateLimiterRes.remainingPoints,
        'X-RateLimit-Reset': Math.ceil(rateLimiterRes.msBeforeNext / 1000)
      });
      
      next();
    })
    .catch(rateLimiterRes => {
      // Trả về lỗi khi vượt quá giới hạn
      const secs = Math.ceil(rateLimiterRes.msBeforeNext / 1000);
      res.set('Retry-After', String(secs));
      
      throw new TooManyRequestsError(
        'Quá nhiều yêu cầu', 
        { 
          retryAfter: secs,
          limit: rateLimiter.points,
          window: rateLimiter.duration
        }
      );
    })
    .catch(next);
};

// Middleware chống tấn công DDoS và brute force
const antiDos = (app) => {
  // Giới hạn kích thước body
  app.use(require('express').json({ limit: '10kb' }));
  
  // Giới hạn kích thước URL
  app.use(require('express').urlencoded({ extended: true, limit: '10kb' }));
  
  // Chống parameter pollution
  const hpp = require('hpp');
  app.use(hpp());
  
  // Chống NoSQL injection
  const mongoSanitize = require('express-mongo-sanitize');
  app.use(mongoSanitize());
  
  // Chống XSS
  const xss = require('xss-clean');
  app.use(xss());
};

module.exports = {
  securityHeaders,
  globalRateLimiter,
  loginRateLimiter,
  registerRateLimiter,
  flexibleRateLimiter,
  antiDos
};
