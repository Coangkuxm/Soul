const { body, validationResult } = require('express-validator');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const { query } = require('../config/db-connection');
const { 
  BadRequestError, 
  NotFoundError, 
  UnauthorizedError,
  DatabaseError
} = require('../utils/errors');
const { sendVerificationEmail: sendVerificationEmailService, sendPasswordResetEmail } = require('../services/email.service');

// Generate a secure random token
const generateToken = (length = 32) => {
  return crypto.randomBytes(Math.ceil(length / 2))
    .toString('hex')
    .slice(0, length);
};

const accountController = {
  // Validation middlewares
  validateForgotPassword: [
    body('email').isEmail().normalizeEmail()
  ],

  validateResetPassword: [
    body('token').notEmpty(),
    body('password').isLength({ min: 6 })
  ],

  validateVerifyEmail: [
    body('token').notEmpty()
  ],

  // Forgot password handler
  async forgotPassword(req, res, next) {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        throw new BadRequestError('Validation failed', errors.array());
      }

      const { email } = req.body;
      const userResult = await query('SELECT * FROM users WHERE email = $1', [email]);
      const user = userResult.rows[0];

      if (!user) {
        return res.json({ 
          success: true, 
          message: 'If your email is registered, you will receive a password reset link.' 
        });
      }

      const resetToken = generateToken();
      const resetTokenExpiry = new Date(Date.now() + 3600000);

      await query(
        'UPDATE users SET reset_password_token = $1, reset_password_expires = $2 WHERE id = $3',
        [resetToken, resetTokenExpiry, user.id]
      );

      await sendPasswordResetEmail(user, resetToken);

      res.json({ 
        success: true, 
        message: 'If your email is registered, you will receive a password reset link.' 
      });
    } catch (error) {
      next(error);
    }
  },

  // Reset password handler
  async resetPassword(req, res, next) {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        throw new BadRequestError('Validation failed', errors.array());
      }

      const { token, password } = req.body;

      // Find user by reset token and check expiry
      const userResult = await query(
        'SELECT * FROM users WHERE reset_password_token = $1 AND reset_password_expires > NOW()',
        [token]
      );
      
      const user = userResult.rows[0];

      if (!user) {
        throw new UnauthorizedError('Invalid or expired reset token');
      }

      // Hash new password
      const hashedPassword = await bcrypt.hash(password, 12);

      // Update password and clear reset token
      await query(
        `UPDATE users 
         SET password = $1, 
             reset_password_token = NULL, 
             reset_password_expires = NULL,
             updated_at = NOW()
         WHERE id = $2`,
        [hashedPassword, user.id]
      );

      res.json({ 
        success: true, 
        message: 'Password has been reset successfully' 
      });
    } catch (error) {
      next(error);
    }
  },

  // Verify email handler
  async verifyEmail(req, res, next) {
    try {
      const { token } = req.body;

      // Verify token and get user
      const userResult = await query(
        `SELECT * FROM users 
         WHERE email_verification_token = $1 
         AND email_verification_expires > NOW() 
         AND email_verified = FALSE`,
        [token]
      );
      
      const user = userResult.rows[0];

      if (!user) {
        throw new UnauthorizedError('Invalid or expired verification token');
      }

      // Mark email as verified and clear token
      await query(
        `UPDATE users 
         SET email_verified = TRUE, 
             email_verification_token = NULL, 
             email_verification_expires = NULL,
             updated_at = NOW()
         WHERE id = $1`,
        [user.id]
      );

      res.json({ 
        success: true, 
        message: 'Email has been verified successfully' 
      });
    } catch (error) {
      next(error);
    }
  },

  // Send verification email handler
  async sendVerificationEmail(req, res, next) {
    try {
      const userId = req.user.id;
      
      // Get user by ID
      const userResult = await query('SELECT * FROM users WHERE id = $1', [userId]);
      const user = userResult.rows[0];

      if (!user) {
        throw new NotFoundError('User not found');
      }

      if (user.email_verified) {
        return res.json({ 
          success: true, 
          message: 'Email is already verified' 
        });
      }

      // Generate verification token and expiry (24 hours from now)
      const verificationToken = generateToken();
      const verificationExpiry = new Date(Date.now() + 24 * 3600000);

      // Save verification token to database
      await query(
        `UPDATE users 
         SET email_verification_token = $1, 
             email_verification_expires = $2 
         WHERE id = $3`,
        [verificationToken, verificationExpiry, userId]
      );

      // Send verification email
      await sendVerificationEmailService(user, verificationToken);

      res.json({ 
        success: true, 
        message: 'Verification email has been sent' 
      });
    } catch (error) {
      next(error);
    }
  },

  // Check email verification status
  async checkEmailVerification(req, res, next) {
    try {
      const userId = req.user.id;
      
      const userResult = await query(
        'SELECT email_verified FROM users WHERE id = $1', 
        [userId]
      );
      
      const user = userResult.rows[0];

      if (!user) {
        throw new NotFoundError('User not found');
      }

      res.json({ 
        success: true, 
        data: { 
          email_verified: user.email_verified 
        } 
      });
    } catch (error) {
      next(error);
    }
  }
};

module.exports = accountController;