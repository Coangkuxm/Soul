const express = require('express');
const router = express.Router();
const socialController = require('../controllers/social.controller');
const { authenticateToken } = require('../middlewares/auth.middleware');

// Follow/Unfollow routes
router.post('/users/:id/follow', authenticateToken, (req, res, next) => 
  socialController.followUser(req, res, next)
);
router.delete('/users/:id/unfollow', authenticateToken, (req, res, next) => 
  socialController.unfollowUser(req, res, next)
);

// Like/Unlike routes
router.post('/likes', authenticateToken, (req, res, next) => 
  socialController.likeItem(req, res, next)
);
router.delete('/likes', authenticateToken, (req, res, next) => 
  socialController.unlikeItem(req, res, next)
);

// Comments routes
router.post('/comments', authenticateToken, (req, res, next) => 
  socialController.createComment(req, res, next)
);
router.get('/comments/:targetType/:targetId', (req, res, next) => 
  socialController.getComments(req, res, next)
);
router.put('/comments/:id', authenticateToken, (req, res, next) => 
  socialController.updateComment(req, res, next)
);
router.delete('/comments/:id', authenticateToken, (req, res, next) => 
  socialController.deleteComment(req, res, next)
);

// Notifications routes
router.get('/notifications', authenticateToken, (req, res, next) => 
  socialController.getNotifications(req, res, next)
);
router.put('/notifications/:id/read', authenticateToken, (req, res, next) => 
  socialController.markAsRead(req, res, next)
);

module.exports = router;
