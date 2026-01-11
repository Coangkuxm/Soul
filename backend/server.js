// server.js
require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const path = require('path');
const rateLimit = require('express-rate-limit');
const cookieParser = require('cookie-parser');
const { body } = require('express-validator');
const { query } = require('./config/db-connection');
// Import routes
const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const collectionsRoutes = require('./routes/collections.routes');
const collectionItemsRoutes = require('./routes/collectionItems.routes');
const itemsRoutes = require('./routes/items.routes');
const spotifyRoutes = require('./routes/spotify.routes');
const accountRoutes = require('./routes/account.routes');
const socialRoutes = require('./routes/social.routes');
const tmdbRoutes = require('./routes/tmdb.routes');
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
app.use(cookieParser());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Log all requests
app.use((req, res, next) => {
  console.log(`${new Date().toISOString()} - ${req.method} ${req.originalUrl}`);
  next();
});

// Swagger UI
const swaggerUiOptions = {
  explorer: true,
  swaggerOptions: {
    persistAuthorization: true,
    oauth2RedirectUrl: `${process.env.API_URL || 'http://localhost:5000'}/api-docs/oauth2-redirect.html`,
    docExpansion: 'none',
    filter: '',
    tagsSorter: 'alpha',
    operationsSorter: 'alpha',
    defaultModelsExpandDepth: 1,
    defaultModelExpandDepth: 1,
    displayRequestDuration: true,
    showCommonExtensions: true,
    showExtensions: true,
    showRequestHeaders: true,
    deepLinking: true
  }
};

app.use('/api-docs', 
  swaggerUi.serve, 
  (req, res, next) => {
    // Ensure the Swagger UI is always served with the latest spec
    swaggerUi.setup(swaggerSpec, swaggerUiOptions)(req, res, next);
  }
);

// Serve OAuth2 redirect HTML for Swagger UI
app.get('/api-docs/oauth2-redirect.html', (req, res) => {
  res.sendFile(path.join(__dirname, 'node_modules/swagger-ui/dist/oauth2-redirect.html'));
});

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Quá nhiều yêu cầu từ địa chỉ IP này, vui lòng thử lại sau 15 phút'
});

// Apply rate limiting to all requests
app.use(limiter);

// API Routes
app.use('/api/spotify', spotifyRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/account', accountRoutes);
app.use('/api/social', socialRoutes);
app.use('/api/users', userRoutes);
app.use('/api/collections', collectionsRoutes);
app.use('/api/collection-items', collectionItemsRoutes);
app.use('/api/items', itemsRoutes);
app.use('/api/tmdb', tmdbRoutes);
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

// Test database connection endpoint
app.get('/test-db', async (req, res) => {
  try {
    console.log('=== TEST DB CONNECTION ===');
    console.log('Bắt đầu test query...');
    
    const startTime = Date.now();
    const result = await query('SELECT NOW() as current_time');
    const duration = Date.now() - startTime;
    
    console.log(`Test query thành công trong ${duration}ms`);
    
    res.json({
      success: true,
      message: 'Database connection OK',
      time: result.rows[0].current_time,
      duration: `${duration}ms`
    });
  } catch (error) {
    console.error('Test query lỗi:', error);
    res.status(500).json({
      success: false,
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