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
  validateChangePassword: [
    body('currentPassword').notEmpty().withMessage('Current password is required'),
    body('newPassword').isLength({ min: 6 }).withMessage('New password must be at least 6 characters long')
  ],
  
  validateForgotPassword: [
    body('email').isEmail().normalizeEmail()
  ],

  validateResetPassword: [
    body('email').isEmail().normalizeEmail(),
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

      // OTP_TEST_MODE: chưa cấu hình SMTP -> trả mã trong response + log (bật mặc định,
      // tắt bằng OTP_TEST_MODE=false khi đã có email thật).
      const testMode = process.env.OTP_TEST_MODE !== 'false';
      const genericMessage = 'Nếu email tồn tại, mã đặt lại mật khẩu đã được gửi.';

      // Không tiết lộ email có tồn tại hay không
      if (!user) {
        return res.json({ success: true, message: genericMessage });
      }

      // Mã OTP 6 chữ số, hết hạn sau 15 phút (lưu chung cột reset_password_token)
      const resetCode = crypto.randomInt(100000, 1000000).toString();
      const resetTokenExpiry = new Date(Date.now() + 15 * 60 * 1000);

      await query(
        'UPDATE users SET reset_password_token = $1, reset_password_expires = $2 WHERE id = $3',
        [resetCode, resetTokenExpiry, user.id]
      );

      console.log(`[ForgotPassword] OTP cho ${email}: ${resetCode} (hết hạn sau 15 phút)`);

      // Cố gắng gửi email; nếu chưa cấu hình SMTP thì bỏ qua trong test mode
      try {
        await sendPasswordResetEmail(user, resetCode);
      } catch (mailErr) {
        console.error('[ForgotPassword] Gửi email thất bại (test mode bỏ qua):', mailErr.message);
        if (!testMode) throw mailErr;
      }

      const payload = { success: true, message: genericMessage };
      if (testMode) payload.devCode = resetCode; // CHỈ dùng để test
      res.json(payload);
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

      const { email, token, password } = req.body;

      // Tìm user theo email + mã OTP còn hạn (scope theo email để hạn chế dò mã 6 số)
      const userResult = await query(
        'SELECT * FROM users WHERE email = $1 AND reset_password_token = $2 AND reset_password_expires > NOW()',
        [email, token]
      );

      const user = userResult.rows[0];

      if (!user) {
        throw new UnauthorizedError('Mã đặt lại không đúng hoặc đã hết hạn');
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
      
      if (userResult.rows.length === 0) {
        throw new NotFoundError('User not found');
      }

      res.json({ 
        success: true, 
        data: { 
          email_verified: userResult.rows[0].email_verified 
        } 
      });
    } catch (error) {
      next(error);
    }
  },

  // Change password with current password
  async changePassword(req, res, next) {
    try {
      const errors = validationResult(req);
      if (!errors.isEmpty()) {
        throw new BadRequestError('Validation failed', errors.array());
      }

      const { currentPassword, newPassword } = req.body;
      const userId = req.user.id;

      // Get user's current password hash
      const userResult = await query('SELECT password FROM users WHERE id = $1', [userId]);
      const user = userResult.rows[0];

      if (!user) {
        throw new NotFoundError('User not found');
      }

      // Verify current password
      const isMatch = await bcrypt.compare(currentPassword, user.password);
      if (!isMatch) {
        throw new UnauthorizedError('Current password is incorrect');
      }

      // Hash new password
      const salt = await bcrypt.genSalt(10);
      const hashedPassword = await bcrypt.hash(newPassword, salt);

      // Update password
      await query('UPDATE users SET password = $1 WHERE id = $2', [hashedPassword, userId]);

      res.json({
        success: true,
        message: 'Password changed successfully'
      });
    } catch (error) {
      next(error);
    }
  }
};

module.exports = accountController;