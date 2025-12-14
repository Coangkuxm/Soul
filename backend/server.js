// server.js
require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');
const rateLimit = require('express-rate-limit');
const { body } = require('express-validator');

// Import routes
const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const collectionsRoutes = require('./routes/collections.routes');

// Import middlewares
const { errorHandler } = require('./middlewares/errorHandler');
const { authenticateToken } = require('./middlewares/auth.middleware');

const app = express();
const PORT = process.env.PORT || 5000;
const swaggerUi = require('swagger-ui-express');
const swaggerSpec = require('./config/swagger');

// ...

// Swagger UI
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec));
// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Quá nhiều yêu cầu từ địa chỉ IP này, vui lòng thử lại sau 15 phút'
});

// Middleware
app.use(cors({
  origin: process.env.CLIENT_URL || '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true
}));

app.use(helmet());
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Apply rate limiting to all requests
app.use(limiter);

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/collections', collectionsRoutes);

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ 
    status: 'OK', 
    message: 'Server đang hoạt động',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
    environment: process.env.NODE_ENV || 'development'
  });
});

// 404 handler
app.use((req, res, next) => {
  res.status(404).json({ 
    success: false, 
    error: 'Không tìm thấy tài nguyên' 
  });
});

// Global error handler
app.use((err, req, res, next) => {
  console.error('Lỗi:', err);
  
  if (err.name === 'UnauthorizedError' || err.statusCode === 401) {
    return res.status(401).json({ 
      success: false, 
      error: err.message || 'Không có quyền truy cập' 
    });
  }

  if (err.name === 'NotFoundError' || err.statusCode === 404) {
    return res.status(404).json({ 
      success: false, 
      error: err.message || 'Không tìm thấy tài nguyên' 
    });
  }

  res.status(err.statusCode || 500).json({
    success: false,
    error: err.message || 'Đã xảy ra lỗi máy chủ'
  });
});

// Khởi động server
app.listen(PORT, () => {
  console.log(`🟢 Server đang chạy tại http://localhost:${PORT}`);
});

module.exports = app;