// server.js
require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');
const rateLimit = require('express-rate-limit');
const { body } = require('express-validator');
const { query } = require('./config/db-connection');
// Import routes
const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const collectionsRoutes = require('./routes/collections.routes');
const collectionItemsRoutes = require('./routes/collectionItems.routes');
const itemsRoutes = require('./routes/items.routes');

// Import middlewares
const { errorHandler } = require('./middlewares/errorHandler');
const { authenticateToken } = require('./middlewares/auth.middleware');

const app = express();
const PORT = process.env.PORT || 5000;
const swaggerUi = require('swagger-ui-express');
const swaggerSpec = require('./config/swagger');

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

// Log all requests
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} - ${req.method} ${req.originalUrl}`);
  next();
});

// Swagger UI
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Quá nhiều yêu cầu từ địa chỉ IP này, vui lòng thử lại sau 15 phút'
});

// Apply rate limiting to all requests
app.use(limiter);

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/collections', collectionsRoutes);
app.use('/api/collections', collectionItemsRoutes); // Add this line
app.use('/api/items', itemsRoutes);

// Health check endpoint
app.get('/health', async (req, res) => {
  try {
    const result = await query('SELECT NOW()');
    res.json({ 
      status: 'ok',
      database: 'connected',
      timestamp: new Date().toISOString(),
      dbTime: result.rows[0].now
    });
  } catch (error) {
    console.error('Health check failed:', error);
    res.status(500).json({ 
      status: 'error',
      database: 'disconnected',
      error: error.message 
    });
  }
});

// 404 handler
app.use((req, res, next) => {
  res.status(404).json({
    success: false,
    error: 'Không tìm thấy tài nguyên'
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Unhandled error:', {
    error: err.message,
    stack: err.stack,
    url: req.originalUrl,
    method: req.method,
    body: req.body
  });
  
  // Handle specific error types
  if (err.name === 'ValidationError') {
    return res.status(400).json({
      success: false,
      error: 'Dữ liệu không hợp lệ',
      details: err.errors
    });
  }

  // Default error handler
  res.status(err.statusCode || 500).json({
    success: false,
    error: err.message || 'Đã xảy ra lỗi máy chủ'
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`🟢 Server đang chạy tại http://localhost:${PORT}`);
});

// Handle unhandled promise rejections
process.on('unhandledRejection', (err) => {
  console.error('Unhandled Rejection:', err);
  // Close server & exit process
  // server.close(() => process.exit(1));
});

module.exports = app;