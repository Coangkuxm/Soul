/**
 * @swagger
 * tags:
 *   name: Account
 *   description: User account management
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     ChangePasswordRequest:
 *       type: object
 *       required:
 *         - currentPassword
 *         - newPassword
 *       properties:
 *         currentPassword:
 *           type: string
 *           format: password
 *           description: User's current password
 *           example: currentPassword123
 *         newPassword:
 *           type: string
 *           format: password
 *           minLength: 6
 *           description: New password (min 6 characters)
 *           example: newSecurePassword123
 */

/**
 * @swagger
 * /account/change-password:
 *   post:
 *     summary: Change user password
 *     tags: [Account]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/ChangePasswordRequest'
 *     responses:
 *       200:
 *         description: Password changed successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 message:
 *                   type: string
 *                   example: Password changed successfully
 *       400:
 *         $ref: '#/components/responses/BadRequest'
 *       401:
 *         $ref: '#/components/responses/Unauthorized'
 *       500:
 *         $ref: '#/components/responses/ServerError'
 */

/**
 * @swagger
 * /account/forgot-password:
 *   post:
 *     summary: Request password reset
 *     description: Sends a password reset link to the user's email
 *     tags: [Account]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - email
 *             properties:
 *               email:
 *                 type: string
 *                 format: email
 *                 example: user@gmail.com
 *     responses:
 *       200:
 *         description: If the email is registered, a reset link will be sent
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 message:
 *                   type: string
 *                   example: If your email is registered, you will receive a password reset link.
 */

/**
 * @swagger
 * /account/reset-password:
 *   post:
 *     summary: Reset password with token
 *     description: Reset password using the token from email
 *     tags: [Account]
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required:
 *               - token
 *               - password
 *             properties:
 *               token:
 *                 type: string
 *                 description: Password reset token from email
 *                 example: a1b2c3d4e5f6g7h8i9j0
 *               password:
 *                 type: string
 *                 format: password
 *                 minLength: 6
 *                 description: New password (min 6 characters)
 *                 example: newSecurePassword123
 *     responses:
 *       200:
 *         description: Password has been reset successfully
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 message:
 *                   type: string
 *                   example: Password has been reset successfully
 */

/**
 * @swagger
 * /account/send-verification-email:
 *   get:
 *     summary: Send verification email
 *     description: Sends a verification email to the logged-in user
 *     tags: [Account]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Verification email has been sent
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 message:
 *                   type: string
 *                   example: Verification email has been sent
 */

/**
 * @swagger
 * /account/check-email-verification:
 *   get:
 *     summary: Check email verification status
 *     description: Check if the logged-in user's email is verified
 *     tags: [Account]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Email verification status
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                   example: true
 *                 data:
 *                   type: object
 *                   properties:
 *                     email_verified:
 *                       type: boolean
 *                       example: true
 */
