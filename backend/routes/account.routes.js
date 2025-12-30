const express = require('express');
const router = express.Router();
const accountController = require('../controllers/account.controller');
const validateRequest = require('../middlewares/validate-request').validateRequest;
const { authenticateToken } = require('../middlewares/auth.middleware');

// Public routes
router.post(
  '/forgot-password',
  accountController.validateForgotPassword,
  validateRequest,
  (req, res, next) => accountController.forgotPassword(req, res, next)
);

router.post(
  '/reset-password',
  accountController.validateResetPassword,
  validateRequest,
  (req, res, next) => accountController.resetPassword(req, res, next)
);

router.post(
  '/verify-email',
  accountController.validateVerifyEmail,
  validateRequest,
  (req, res, next) => accountController.verifyEmail(req, res, next)
);

// Protected routes
router.get(
  '/send-verification-email',
  authenticateToken,
  (req, res, next) => accountController.sendVerificationEmail(req, res, next)
);

router.get(
  '/check-email-verification',
  authenticateToken,
  (req, res, next) => accountController.checkEmailVerification(req, res, next)
);

module.exports = router;