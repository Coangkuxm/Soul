const { validationResult } = require('express-validator');
const { ValidationError } = require('../utils/errors');

/**
 * Middleware factory that returns a validation middleware
 * @param {Array} validations - Array of validation chains
 * @returns {Function} Express middleware function
 */
const validate = (validations) => {
  return async (req, res, next) => {
    // Run all validations
    await Promise.all(validations.map(validation => validation.run(req)));

    // Check for validation errors
    const errors = validationResult(req);
    if (errors.isEmpty()) {
      return next();
    }

    // Format errors
    const extractedErrors = errors.array().map(err => ({
      field: err.path,
      message: err.msg
    }));

    // Throw validation error
    throw new ValidationError('Dữ liệu không hợp lệ', extractedErrors);
  };
};

module.exports = {
  validate
};