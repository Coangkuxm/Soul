class CustomError extends Error {
  constructor(message, statusCode, code, details = {}) {
    super(message);
    this.statusCode = statusCode;
    this.code = code || `ERR_${this.constructor.name.toUpperCase()}`;
    this.details = details;
    this.timestamp = new Date().toISOString();
    this.name = this.constructor.name;
    
    // Chỉ capture stack trace trong môi trường development
    if (process.env.NODE_ENV === 'development') {
      Error.captureStackTrace(this, this.constructor);
    } else {
      this.stack = undefined; // Ẩn stack trace trong production
    }
  }

  toJSON() {
    return {
      error: {
        name: this.name,
        code: this.code,
        message: this.message,
        ...(Object.keys(this.details).length && { details: this.details }),
        ...(this.stack && { stack: this.stack })
      },
      timestamp: this.timestamp
    };
  }
}

// 4xx Client Errors
class BadRequestError extends CustomError {
  constructor(message = 'Yêu cầu không hợp lệ', details = {}) {
    super(message, 400, 'BAD_REQUEST', details);
  }
}

class UnauthorizedError extends CustomError {
  constructor(message = 'Không có quyền truy cập', details = {}) {
    super(message, 401, 'UNAUTHORIZED', details);
  }
}

class PaymentRequiredError extends CustomError {
  constructor(message = 'Yêu cầu thanh toán', details = {}) {
    super(message, 402, 'PAYMENT_REQUIRED', details);
  }
}

class ForbiddenError extends CustomError {
  constructor(message = 'Truy cập bị từ chối', details = {}) {
    super(message, 403, 'FORBIDDEN', details);
  }
}

class NotFoundError extends CustomError {
  constructor(message = 'Không tìm thấy tài nguyên', details = {}) {
    super(message, 404, 'NOT_FOUND', details);
  }
}

class MethodNotAllowedError extends CustomError {
  constructor(message = 'Phương thức không được hỗ trợ', details = {}) {
    super(message, 405, 'METHOD_NOT_ALLOWED', details);
  }
}

class NotAcceptableError extends CustomError {
  constructor(message = 'Không thể xử lý yêu cầu', details = {}) {
    super(message, 406, 'NOT_ACCEPTABLE', details);
  }
}

class ConflictError extends CustomError {
  constructor(message = 'Xung đột dữ liệu', details = {}) {
    super(message, 409, 'CONFLICT', details);
  }
}

class TooManyRequestsError extends CustomError {
  constructor(message = 'Quá nhiều yêu cầu', details = {}) {
    super(message, 429, 'TOO_MANY_REQUESTS', details);
  }
}

// 5xx Server Errors
class InternalServerError extends CustomError {
  constructor(message = 'Lỗi máy chủ nội bộ', details = {}) {
    super(message, 500, 'INTERNAL_SERVER_ERROR', details);
  }
}

class NotImplementedError extends CustomError {
  constructor(message = 'Tính năng chưa được triển khai', details = {}) {
    super(message, 501, 'NOT_IMPLEMENTED', details);
  }
}

class ServiceUnavailableError extends CustomError {
  constructor(message = 'Dịch vụ tạm thởi gián đoạn', details = {}) {
    super(message, 503, 'SERVICE_UNAVAILABLE', details);
  }
}

// Database Errors
class DatabaseError extends CustomError {
  constructor(message = 'Lỗi cơ sở dữ liệu', details = {}) {
    super(message, 500, 'DATABASE_ERROR', details);
  }
}

class ValidationError extends CustomError {
  constructor(errors = [], message = 'Dữ liệu không hợp lệ') {
    super(message, 400, 'VALIDATION_ERROR', { errors });
    this.errors = errors;
  }
}

// Authentication & Authorization
class AuthenticationError extends UnauthorizedError {
  constructor(message = 'Xác thực thất bại', details = {}) {
    super(message, { ...details, code: 'AUTHENTICATION_FAILED' });
  }
}

class InvalidTokenError extends UnauthorizedError {
  constructor(message = 'Token không hợp lệ hoặc đã hết hạn', details = {}) {
    super(message, { ...details, code: 'INVALID_TOKEN' });
  }
}

class InsufficientPermissionsError extends ForbiddenError {
  constructor(message = 'Không đủ quyền thực hiện hành động', details = {}) {
    super(message, { ...details, code: 'INSUFFICIENT_PERMISSIONS' });
  }
}

// Business Logic Errors
class ResourceNotFoundError extends NotFoundError {
  constructor(resource, id, details = {}) {
    super(`${resource} không tồn tại`, { 
      ...details, 
      resource,
      id,
      code: 'RESOURCE_NOT_FOUND' 
    });
  }
}

class ResourceAlreadyExistsError extends ConflictError {
  constructor(resource, details = {}) {
    super(`${resource} đã tồn tại`, { 
      ...details, 
      resource,
      code: 'RESOURCE_EXISTS' 
    });
  }
}

// Utility function to handle async/await errors
const asyncHandler = (fn) => (req, res, next) => {
  Promise.resolve(fn(req, res, next)).catch(next);
};

// Error handling middleware
const errorHandler = (err, req, res, next) => {
  // Set default values for unknown errors
  if (!err.statusCode) {
    console.error('Unhandled error:', err);
    err = new InternalServerError('Đã xảy ra lỗi không xác định');
  }

  // Log the error
  if (process.env.NODE_ENV === 'development') {
    console.error(`[${err.name}] ${err.message}\n${err.stack}`);
  }

  // Send error response
  res.status(err.statusCode).json({
    success: false,
    error: {
      code: err.code,
      message: err.message,
      ...(process.env.NODE_ENV === 'development' && { stack: err.stack }),
      ...(err.details && { details: err.details })
    },
    timestamp: new Date().toISOString()
  });
};

module.exports = {
  // Base Error
  CustomError,
  
  // 4xx Client Errors
  BadRequestError,
  UnauthorizedError,
  PaymentRequiredError,
  ForbiddenError,
  NotFoundError,
  MethodNotAllowedError,
  NotAcceptableError,
  ConflictError,
  TooManyRequestsError,
  
  // 5xx Server Errors
  InternalServerError,
  NotImplementedError,
  ServiceUnavailableError,
  
  // Database
  DatabaseError,
  ValidationError,
  
  // Auth
  AuthenticationError,
  InvalidTokenError,
  InsufficientPermissionsError,
  
  // Business Logic
  ResourceNotFoundError,
  ResourceAlreadyExistsError,
  
  // Middleware
  asyncHandler,
  errorHandler
};