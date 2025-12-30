const db = require('../config/db');
const { BadRequestError, UnauthorizedError, NotFoundError } = require('../utils/errors');

class SocialController {
  // Follow a user
  async followUser(req, res, next) {
    try {
      const userId = req.user.id;
      const followingId = parseInt(req.params.id);

      if (userId === followingId) {
        throw new BadRequestError('You cannot follow yourself');
      }

      // Check if already following
      const existingFollow = await db.query(
        'SELECT * FROM user_follows WHERE follower_id = $1 AND following_id = $2',
        [userId, followingId]
      );

      if (existingFollow.rows.length > 0) {
        throw new BadRequestError('Already following this user');
      }

      // Create follow relationship
      await db.query(
        'INSERT INTO user_follows (follower_id, following_id) VALUES ($1, $2)',
        [userId, followingId]
      );

      // Record activity
      await db.query(
        'INSERT INTO activities (user_id, activity_type, target_user_id) VALUES ($1, $2, $3)',
        [userId, 'follow', followingId]
      );

      // Create notification
      await db.query(
        `INSERT INTO notifications 
        (recipient_id, notification_type, sender_id, target_id, target_type) 
        VALUES ($1, $2, $3, $4, 'user')`,
        [followingId, 'follow', userId, userId]
      );

      res.status(200).json({ success: true, message: 'Successfully followed user' });
    } catch (error) {
      next(error);
    }
  }

  // Unfollow a user
  async unfollowUser(req, res, next) {
    try {
      const userId = req.user.id;
      const followingId = parseInt(req.params.id);

      const result = await db.query(
        'DELETE FROM user_follows WHERE follower_id = $1 AND following_id = $2 RETURNING *',
        [userId, followingId]
      );

      if (result.rowCount === 0) {
        throw new NotFoundError('Follow relationship not found');
      }

      res.status(200).json({ success: true, message: 'Successfully unfollowed user' });
    } catch (error) {
      next(error);
    }
  }

  // Like an item/collection/comment
  async likeItem(req, res, next) {
    try {
      const { targetId, targetType } = req.body;
      const userId = req.user.id;

      // Validate target type
      if (!['collection', 'item', 'comment'].includes(targetType)) {
        throw new BadRequestError('Invalid target type');
      }

      // Check if already liked
      const existingLike = await db.query(
        'SELECT * FROM likes WHERE user_id = $1 AND target_id = $2 AND target_type = $3',
        [userId, targetId, targetType]
      );

      if (existingLike.rows.length > 0) {
        throw new BadRequestError('Already liked this item');
      }

      // Create like
      await db.query(
        'INSERT INTO likes (user_id, target_id, target_type) VALUES ($1, $2, $3)',
        [userId, targetId, targetType]
      );

      // Update like count in the respective table
      await db.query(
        `UPDATE ${targetType}s SET like_count = like_count + 1 WHERE id = $1`,
        [targetId]
      );

      // Record activity
      await db.query(
        'INSERT INTO activities (user_id, activity_type, target_id, target_type) VALUES ($1, $2, $3, $4)',
        [userId, 'like', targetId, targetType]
      );

      // Create notification
      let targetUserId;
      if (targetType === 'comment') {
        const comment = await db.query('SELECT user_id FROM comments WHERE id = $1', [targetId]);
        targetUserId = comment.rows[0]?.user_id;
      } else {
        const item = await db.query(`SELECT user_id, owner_id FROM ${targetType}s WHERE id = $1`, [targetId]);
        targetUserId = targetType === 'collection' ? item.rows[0]?.owner_id : item.rows[0]?.user_id;
      }

      if (targetUserId && targetUserId !== userId) {
        await db.query(
          `INSERT INTO notifications 
          (recipient_id, notification_type, sender_id, target_id, target_type) 
          VALUES ($1, $2, $3, $4, $5)`,
          [targetUserId, 'like', userId, targetId, targetType]
        );
      }

      res.status(201).json({ success: true, message: 'Successfully liked item' });
    } catch (error) {
      next(error);
    }
  }

  // Unlike an item/collection/comment
  async unlikeItem(req, res, next) {
    try {
      const { targetId, targetType } = req.body;
      const userId = req.user.id;

      // Validate target type
      if (!['collection', 'item', 'comment'].includes(targetType)) {
        throw new BadRequestError('Invalid target type');
      }

      const result = await db.query(
        'DELETE FROM likes WHERE user_id = $1 AND target_id = $2 AND target_type = $3 RETURNING *',
        [userId, targetId, targetType]
      );

      if (result.rowCount === 0) {
        throw new NotFoundError('Like not found');
      }

      // Update like count in the respective table
      await db.query(
        `UPDATE ${targetType}s SET like_count = GREATEST(0, like_count - 1) WHERE id = $1`,
        [targetId]
      );

      res.status(200).json({ success: true, message: 'Successfully unliked item' });
    } catch (error) {
      next(error);
    }
  }

  // Create a comment
  async createComment(req, res, next) {
    try {
      const { content, targetId, targetType } = req.body;
      const userId = req.user.id;

      // Validate target type
      if (!['collection', 'item'].includes(targetType)) {
        throw new BadRequestError('Invalid target type');
      }

      // Check if target exists
      const targetExists = await db.query(
        `SELECT id FROM ${targetType}s WHERE id = $1`,
        [targetId]
      );

      if (targetExists.rows.length === 0) {
        throw new NotFoundError(`${targetType} not found`);
      }

      // Create comment
      const result = await db.query(
        `INSERT INTO comments 
        (user_id, content, target_id, target_type) 
        VALUES ($1, $2, $3, $4) 
        RETURNING *`,
        [userId, content, targetId, targetType]
      );

      const comment = result.rows[0];

      // Update comment count in the respective table
      await db.query(
        `UPDATE ${targetType}s 
        SET comment_count = comment_count + 1 
        WHERE id = $1`,
        [targetId]
      );

      // Record activity
      await db.query(
        'INSERT INTO activities (user_id, activity_type, target_id, target_type) VALUES ($1, $2, $3, $4)',
        [userId, 'comment', comment.id, 'comment']
      );

      // Create notification for the target owner
      let targetOwnerId;
      if (targetType === 'collection') {
        const collection = await db.query(
          'SELECT owner_id FROM collections WHERE id = $1',
          [targetId]
        );
        targetOwnerId = collection.rows[0]?.owner_id;
      } else {
        const item = await db.query(
          'SELECT created_by FROM items WHERE id = $1',
          [targetId]
        );
        targetOwnerId = item.rows[0]?.created_by;
      }

      if (targetOwnerId && targetOwnerId !== userId) {
        await db.query(
          `INSERT INTO notifications 
          (recipient_id, notification_type, sender_id, target_id, target_type) 
          VALUES ($1, $2, $3, $4, $5)`,
          [targetOwnerId, 'comment', userId, targetId, targetType]
        );
      }

      res.status(201).json({ success: true, data: comment });
    } catch (error) {
      next(error);
    }
  }

  // Get comments for a target
  async getComments(req, res, next) {
    try {
      const { targetType, targetId } = req.params;
      const { page = 1, limit = 10 } = req.query;
      const offset = (page - 1) * limit;

      // Validate target type
      if (!['collection', 'item'].includes(targetType)) {
        throw new BadRequestError('Invalid target type');
      }

      // Get comments with user info
      const result = await db.query(
        `SELECT c.*, u.username, u.avatar_url 
        FROM comments c
        JOIN users u ON c.user_id = u.id
        WHERE c.target_id = $1 AND c.target_type = $2
        ORDER BY c.created_at DESC
        LIMIT $3 OFFSET $4`,
        [targetId, targetType, limit, offset]
      );

      // Get total count for pagination
      const countResult = await db.query(
        'SELECT COUNT(*) FROM comments WHERE target_id = $1 AND target_type = $2',
        [targetId, targetType]
      );

      const total = parseInt(countResult.rows[0].count);
      const totalPages = Math.ceil(total / limit);

      res.status(200).json({
        success: true,
        data: result.rows,
        pagination: {
          total,
          page: parseInt(page),
          limit: parseInt(limit),
          totalPages
        }
      });
    } catch (error) {
      next(error);
    }
  }

  // Update a comment
  async updateComment(req, res, next) {
    try {
      const { id } = req.params;
      const { content } = req.body;
      const userId = req.user.id;

      // Check if comment exists and user is the owner
      const comment = await db.query(
        'SELECT * FROM comments WHERE id = $1',
        [id]
      );

      if (comment.rows.length === 0) {
        throw new NotFoundError('Comment not found');
      }

      if (comment.rows[0].user_id !== userId) {
        throw new UnauthorizedError('Not authorized to update this comment');
      }

      // Update comment
      const result = await db.query(
        `UPDATE comments 
        SET content = $1, updated_at = NOW() 
        WHERE id = $2 
        RETURNING *`,
        [content, id]
      );

      res.status(200).json({ success: true, data: result.rows[0] });
    } catch (error) {
      next(error);
    }
  }

  // Delete a comment
  async deleteComment(req, res, next) {
    try {
      const { id } = req.params;
      const userId = req.user.id;

      // Check if comment exists and get its details
      const comment = await db.query(
        'SELECT * FROM comments WHERE id = $1',
        [id]
      );

      if (comment.rows.length === 0) {
        throw new NotFoundError('Comment not found');
      }

      // Only the comment owner or admin can delete
      if (comment.rows[0].user_id !== userId && req.user.role !== 'admin') {
        throw new UnauthorizedError('Not authorized to delete this comment');
      }

      // Delete comment
      await db.query('DELETE FROM comments WHERE id = $1', [id]);

      // Update comment count in the respective table
      const { target_id, target_type } = comment.rows[0];
      await db.query(
        `UPDATE ${target_type}s 
        SET comment_count = GREATEST(0, comment_count - 1) 
        WHERE id = $1`,
        [target_id]
      );

      res.status(200).json({ success: true, message: 'Comment deleted successfully' });
    } catch (error) {
      next(error);
    }
  }

  // Get user's notifications
  async getNotifications(req, res, next) {
    try {
      const userId = req.user.id;
      const { page = 1, limit = 20, unreadOnly = false } = req.query;
      const offset = (page - 1) * limit;

      let query = `
        SELECT n.*, 
               u.username as sender_username,
               u.avatar_url as sender_avatar
        FROM notifications n
        JOIN users u ON n.sender_id = u.id
        WHERE n.recipient_id = $1
      `;

      const params = [userId];
      let paramCount = 1;

      if (unreadOnly === 'true') {
        paramCount++;
        query += ` AND n.is_read = $${paramCount}`;
        params.push(false);
      }

      // Add ordering and pagination
      query += `
        ORDER BY n.created_at DESC
        LIMIT $${paramCount + 1} OFFSET $${paramCount + 2}
      `;
      params.push(parseInt(limit), offset);

      // Get notifications
      const result = await db.query(query, params);

      // Get total count for pagination
      let countQuery = 'SELECT COUNT(*) FROM notifications WHERE recipient_id = $1';
      const countParams = [userId];
      
      if (unreadOnly === 'true') {
        countQuery += ' AND is_read = $2';
        countParams.push(false);
      }

      const countResult = await db.query(countQuery, countParams);
      const total = parseInt(countResult.rows[0].count);
      const totalPages = Math.ceil(total / limit);

      res.status(200).json({
        success: true,
        data: result.rows,
        pagination: {
          total,
          page: parseInt(page),
          limit: parseInt(limit),
          totalPages
        }
      });
    } catch (error) {
      next(error);
    }
  }

  // Mark notification as read
  async markAsRead(req, res, next) {
    try {
      const { id } = req.params;
      const userId = req.user.id;

      // Mark notification as read
      const result = await db.query(
        `UPDATE notifications 
        SET is_read = true 
        WHERE id = $1 AND recipient_id = $2
        RETURNING *`,
        [id, userId]
      );

      if (result.rows.length === 0) {
        throw new NotFoundError('Notification not found or access denied');
      }

      res.status(200).json({ success: true, data: result.rows[0] });
    } catch (error) {
      next(error);
    }
  }
}

module.exports = new SocialController();
