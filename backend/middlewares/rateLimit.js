const rateLimit = require('express-rate-limit');

// Tạo rate limiter cho TMDB API (40 requests/10s)
const tmdbRateLimiter = rateLimit({
  windowMs: 10 * 1000, // 10 giây
  max: 40, // Giới hạn 40 requests mỗi 10 giây
  message: 'Quá nhiều yêu cầu tới TMDB API, vui lòng thử lại sau 10 giây',
  standardHeaders: true,
  legacyHeaders: false,
});

module.exports = {
  tmdbRateLimiter
};
