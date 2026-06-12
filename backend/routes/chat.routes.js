const express = require('express');
const router = express.Router();
const { body, param, query } = require('express-validator');
const chatController = require('../controllers/chat.controller');
const { authenticateToken } = require('../middlewares/auth.middleware');
const { validate } = require('../middlewares/validation.middleware');

router.use(authenticateToken);

router.post(
  '/conversations',
  validate([
    body('targetUserId').isInt({ min: 1 }).withMessage('targetUserId is required')
  ]),
  chatController.createOrGetDirectConversation
);

router.get(
  '/conversations',
  validate([
    query('page').optional().isInt({ min: 1 }).toInt(),
    query('limit').optional().isInt({ min: 1, max: 100 }).toInt()
  ]),
  chatController.getConversations
);

router.get(
  '/conversations/:id/messages',
  validate([
    param('id').isInt({ min: 1 }).withMessage('conversation id is invalid'),
    query('page').optional().isInt({ min: 1 }).toInt(),
    query('limit').optional().isInt({ min: 1, max: 100 }).toInt()
  ]),
  chatController.getMessages
);

router.post(
  '/conversations/:id/messages',
  validate([
    param('id').isInt({ min: 1 }).withMessage('conversation id is invalid'),
    body('content').isString().trim().isLength({ min: 1 }).withMessage('content is required'),
    body('message_type').optional().isIn(['text', 'image', 'system', 'shared_post']),
    body('metadata').optional().isObject(),
    body('reply_to_message_id').optional().isInt({ min: 1 })
  ]),
  chatController.sendMessage
);

router.put(
  '/conversations/:id/read',
  validate([
    param('id').isInt({ min: 1 }).withMessage('conversation id is invalid')
  ]),
  chatController.markConversationRead
);

module.exports = router;
