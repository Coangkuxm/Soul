const { validationResult } = require('express-validator');
const { BadRequestError } = require('../utils/errors');

/**
 * Middleware to validate the request using express-validator
 * @param {Object} req - Express request object
 * @param {Object} res - Express response object
 * @param {Function} next - Express next middleware function
 */
const validateRequest = (req, res, next) => {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    throw new BadRequestError('Validation failed', errors.array());
  }
  next();
};

module.exports = { validateRequest };
