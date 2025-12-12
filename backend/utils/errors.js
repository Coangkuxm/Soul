class CustomError extends Error {
  constructor(message, statusCode) {
    super(message);
    this.statusCode = statusCode;
    this.name = this.constructor.name;
    Error.captureStackTrace(this, this.constructor);
  }
}

class UnauthorizedError extends CustomError {
  constructor(message = 'Không có quyền truy cập') {
    super(message, 401);
  }
}

class NotFoundError extends CustomError {
  constructor(message = 'Không tìm thấy tài nguyên') {
    super(message, 404);
  }
}

class ValidationError extends CustomError {
  constructor(message = 'Dữ liệu không hợp lệ', errors = []) {
    super(message, 400);
    this.errors = errors;
  }
}

class ConflictError extends CustomError {
  constructor(message = 'Tài nguyên đã tồn tại') {
    super(message, 409);
  }
}

module.exports = {
  CustomError,
  UnauthorizedError,
  NotFoundError,
  ValidationError,
  ConflictError
};