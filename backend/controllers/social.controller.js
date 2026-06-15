const db = require('../config/db-connection');
const { BadRequestError, UnauthorizedError, NotFoundError } = require('../utils/errors');

class SocialController {
  // Get feed - random items from collection_items (friends first, then others)
  async getFeed(req, res, next) {
    try {
      const userId = req.user.id;
      const page = parseInt(req.query.page) || 1;
      const limit = parseInt(req.query.limit) || 10;
      const offset = (page - 1) * limit;

      // scope=friends -> chỉ bài của người mình đang follow (bạn bè); mặc định: mọi người
      const friendsOnly = req.query.scope === 'friends';
      const friendFilter = friendsOnly ? 'AND uf.follower_id IS NOT NULL' : '';

      const feedQuery = `
        WITH user_preferences AS (
          SELECT i.type, COUNT(*)::int AS weight
          FROM likes l
          JOIN items i ON i.id = l.target_id
          WHERE l.user_id = $1
            AND l.target_type = 'item'
          GROUP BY i.type

          UNION ALL

          SELECT i.type, COUNT(*)::int AS weight
          FROM comments cm
          JOIN collection_items ci_pref
            ON cm.target_type = 'collection_item'
           AND cm.target_id = ci_pref.id
          JOIN items i ON i.id = ci_pref.item_id
          WHERE cm.user_id = $1
          GROUP BY i.type
        ),
        preference_scores AS (
          SELECT type, SUM(weight)::int AS weight
          FROM user_preferences
          GROUP BY type
        ),
        post_stats AS (
          SELECT
            ci.id AS collection_item_id,
            COUNT(DISTINCT lp.id)::int AS post_like_count,
            COUNT(DISTINCT cm.id)::int AS post_comment_count
          FROM collection_items ci
          LEFT JOIN likes lp
            ON lp.target_type = 'item'
           AND lp.target_id = ci.item_id
          LEFT JOIN comments cm
            ON cm.target_type = 'collection_item'
           AND cm.target_id = ci.id
          GROUP BY ci.id
        )
        SELECT 
          ci.id as collection_item_id,
          ci.note,
          ci.rating,
          ci.added_at,
          i.id as item_id,
          i.type as item_type,
          i.title,
          i.description,
          i.cover_image_url,
          i.metadata,
          c.id as collection_id,
          c.name as collection_name,
          u.id as user_id,
          u.username,
          u.display_name,
          u.avatar_url,
          CASE WHEN uf.follower_id IS NOT NULL THEN true ELSE false END as is_friend,
          CASE WHEN l.id IS NOT NULL THEN true ELSE false END as is_liked,
          (
            CASE WHEN uf.follower_id IS NOT NULL THEN 80 ELSE 0 END
            + CASE WHEN c.owner_id = $1 THEN -100 ELSE 0 END
            + CASE
                WHEN ci.added_at >= NOW() - INTERVAL '1 day' THEN 45
                WHEN ci.added_at >= NOW() - INTERVAL '7 days' THEN 30
                WHEN ci.added_at >= NOW() - INTERVAL '30 days' THEN 12
                ELSE 0
              END
            + LEAST(COALESCE(ps.post_like_count, 0), 20) * 2
            + LEAST(COALESCE(ps.post_comment_count, 0), 20) * 3
            + LEAST(COALESCE(pref.weight, 0), 10) * 4
            + RANDOM() * 8
          ) as feed_score
        FROM collection_items ci
        JOIN items i ON ci.item_id = i.id
        JOIN collections c ON ci.collection_id = c.id
        JOIN users u ON c.owner_id = u.id
        LEFT JOIN user_follows uf ON uf.following_id = u.id AND uf.follower_id = $1
        LEFT JOIN likes l ON l.target_id = i.id AND l.target_type = 'item' AND l.user_id = $1
        LEFT JOIN preference_scores pref ON pref.type = i.type
        LEFT JOIN post_stats ps ON ps.collection_item_id = ci.id
        WHERE c.is_private = false
          AND COALESCE(u.account_status, 'active') = 'active'
          AND COALESCE(ci.moderation_status, 'active') = 'active'
          AND ci.added_at >= NOW() - INTERVAL '180 days'
          ${friendFilter}
        ORDER BY
          feed_score DESC,
          ci.added_at DESC
        LIMIT $2 OFFSET $3
      `;

      const result = await db.query(feedQuery, [userId, limit, offset]);
      
      const feedItems = result.rows.map(row => ({
        id: row.collection_item_id,
        item: {
          id: row.item_id,
          type: row.item_type,
          title: row.title,
          description: row.description,
          coverImageUrl: row.cover_image_url,
          metadata: row.metadata
        },
        note: row.note,
        rating: row.rating,
        addedAt: row.added_at,
        collection: {
          id: row.collection_id,
          name: row.collection_name
        },
        user: {
          id: row.user_id,
          username: row.username,
          displayName: row.display_name,
          avatarUrl: row.avatar_url
        },
        isFriend: row.is_friend,
        isLiked: row.is_liked,
        activityText: getActivityText(row.item_type)
      }));

      res.json({
        success: true,
        data: feedItems,
        pagination: {
          page,
          limit,
          hasMore: feedItems.length === limit
        }
      });
    } catch (error) {
      next(error);
    }
  }

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
      const { content, targetId, targetType, parentId } = req.body;
      const userId = req.user.id;

      // Validate target type
      if (!['collection', 'item', 'collection_item'].includes(targetType)) {
        throw new BadRequestError('Invalid target type');
      }

      const targetTable = getCommentTargetTable(targetType);

      // Check if target exists
      const targetExists = await db.query(
        `SELECT id FROM ${targetTable} WHERE id = $1`,
        [targetId]
      );

      if (targetExists.rows.length === 0) {
        throw new NotFoundError(`${targetType} not found`);
      }

      let normalizedParentId = null;
      if (parentId) {
        const parentCommentResult = await db.query(
          `SELECT id, parent_id, target_id, target_type
           FROM comments
           WHERE id = $1`,
          [parentId]
        );

        if (parentCommentResult.rows.length === 0) {
          throw new NotFoundError('Parent comment not found');
        }

        const parentComment = parentCommentResult.rows[0];
        if (parentComment.target_id !== targetId || parentComment.target_type !== targetType) {
          throw new BadRequestError('Parent comment does not belong to this target');
        }

        // Flatten deep nesting to one visible reply level.
        normalizedParentId = parentComment.parent_id || parentComment.id;
      }

      // Create comment
      const result = await db.query(
        `INSERT INTO comments 
        (user_id, content, target_id, target_type, parent_id) 
        VALUES ($1, $2, $3, $4, $5) 
        RETURNING *`,
        [userId, content, targetId, targetType, normalizedParentId]
      );

      const comment = result.rows[0];

      // Only collections currently have comment_count column.
      if (targetType === 'collection') {
        await db.query(
          `UPDATE collections
          SET comment_count = comment_count + 1
          WHERE id = $1`,
          [targetId]
        );
      }

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
      } else if (targetType === 'collection_item') {
        const collectionItem = await db.query(
          `SELECT c.owner_id
           FROM collection_items ci
           JOIN collections c ON c.id = ci.collection_id
           WHERE ci.id = $1`,
          [targetId]
        );
        targetOwnerId = collectionItem.rows[0]?.owner_id;
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
      const userId = req.user.id;
      const offset = (page - 1) * limit;

      // Validate target type
      if (!['collection', 'item', 'collection_item'].includes(targetType)) {
        throw new BadRequestError('Invalid target type');
      }

      // Get comments with user info
      const result = await db.query(
        `SELECT c.*,
                u.username,
                u.avatar_url,
                CASE WHEN l.id IS NOT NULL THEN true ELSE false END AS is_liked
        FROM comments c
        JOIN users u ON c.user_id = u.id
        LEFT JOIN likes l
          ON l.target_id = c.id
         AND l.target_type = 'comment'
         AND l.user_id = $3
        WHERE c.target_id = $1
          AND c.target_type = $2
          AND COALESCE(u.account_status, 'active') = 'active'
        ORDER BY
          COALESCE(c.parent_id, c.id) DESC,
          CASE WHEN c.parent_id IS NULL THEN 0 ELSE 1 END,
          c.created_at ASC
        LIMIT $4 OFFSET $5`,
        [targetId, targetType, userId, limit, offset]
      );

      // Get total count for pagination
      const countResult = await db.query(
        `SELECT COUNT(*)
         FROM comments c
         JOIN users u ON c.user_id = u.id
         WHERE c.target_id = $1
           AND c.target_type = $2
           AND COALESCE(u.account_status, 'active') = 'active'`,
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

      // Only collections currently have comment_count column.
      const { target_id, target_type } = comment.rows[0];
      if (target_type === 'collection') {
        await db.query(
          `UPDATE collections
          SET comment_count = GREATEST(0, comment_count - 1)
          WHERE id = $1`,
          [target_id]
        );
      }

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
          AND COALESCE(u.account_status, 'active') = 'active'
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
      let countQuery = `
        SELECT COUNT(*)
        FROM notifications n
        JOIN users u ON u.id = n.sender_id
        WHERE n.recipient_id = $1
          AND COALESCE(u.account_status, 'active') = 'active'`;
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

// Helper function for activity text
function getActivityText(itemType) {
  const texts = {
    'music': 'đang nghe một bài hát mới',
    'movie': 'đang xem một bộ phim mới',
    'book': 'đang đọc một cuốn sách mới',
    'game': 'đang chơi một game mới',
    'artist': 'đã thêm một nghệ sĩ mới',
    'other': 'đã thêm một item mới'
  };
  return texts[itemType] || texts['other'];
}

function getCommentTargetTable(targetType) {
  const tableMap = {
    collection: 'collections',
    item: 'items',
    collection_item: 'collection_items'
  };

  return tableMap[targetType];
}

module.exports = new SocialController();

