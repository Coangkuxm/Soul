const express = require('express');
const router = express.Router();
const userController = require('../controllers/user.controller');
const { authenticateToken } = require('../middlewares/auth.middleware');
const { validate } = require('../middlewares/validation.middleware');

// Public routes
router.post('/register', validate('register'), userController.register);

// Protected routes (require authentication)
router.use(authenticateToken);

// User profile
router.get('/profile', userController.getProfile);
router.put('/profile', validate('updateProfile'), userController.updateProfile);
router.put('/password', validate('changePassword'), userController.changePassword);

// Admin routes
router.get('/', userController.getAllUsers);
router.get('/:id', userController.getUserById);
router.put('/:id', validate('updateUser'), userController.updateUser);
router.delete('/:id', userController.deleteUser);

module.exports = router;