const { query, getClient } = require('../config/db-connection');
const { BadRequestError, ForbiddenError, NotFoundError } = require('../utils/errors');

const parsePage = (value, fallback) => {
  const n = parseInt(value, 10);
  return Number.isFinite(n) && n > 0 ? n : fallback;
};

const ensureMembership = async (conversationId, userId) => {
  const rs = await query(
    `SELECT conversation_id, user_id, last_read_at
     FROM conversation_members
     WHERE conversation_id = $1 AND user_id = $2`,
    [conversationId, userId]
  );
  if (rs.rows.length === 0) {
    throw new ForbiddenError('You are not a member of this conversation');
  }
  return rs.rows[0];
};

const createOrGetDirectConversation = async (req, res, next) => {
  const client = await getClient();
  try {
    const currentUserId = req.user.id;
    const targetUserId = parseInt(req.body.targetUserId, 10);

    if (!Number.isFinite(targetUserId) || targetUserId <= 0) {
      throw new BadRequestError('targetUserId is required');
    }
    if (currentUserId === targetUserId) {
      throw new BadRequestError('Cannot create direct conversation with yourself');
    }

    const userExists = await client.query(
      `SELECT id
       FROM users
       WHERE id = $1
         AND COALESCE(account_status, 'active') = 'active'`,
      [targetUserId]
    );
    if (userExists.rows.length === 0) {
      throw new NotFoundError('Target user not found');
    }

    const userA = Math.min(currentUserId, targetUserId);
    const userB = Math.max(currentUserId, targetUserId);

    const existing = await client.query(
      `SELECT c.id, c.conversation_type, c.created_at, c.updated_at, c.last_message_at
       FROM direct_conversations dc
       JOIN conversations c ON c.id = dc.conversation_id
       WHERE dc.user_a_id = $1 AND dc.user_b_id = $2`,
      [userA, userB]
    );

    if (existing.rows.length > 0) {
      return res.status(200).json({
        success: true,
        data: existing.rows[0]
      });
    }

    await client.query('BEGIN');

    const conv = await client.query(
      `INSERT INTO conversations (conversation_type, created_by)
       VALUES ('direct', $1)
       RETURNING id, conversation_type, created_at, updated_at, last_message_at`,
      [currentUserId]
    );
    const conversation = conv.rows[0];

    await client.query(
      `INSERT INTO direct_conversations (conversation_id, user_a_id, user_b_id)
       VALUES ($1, $2, $3)`,
      [conversation.id, userA, userB]
    );

    await client.query(
      `INSERT INTO conversation_members (conversation_id, user_id, role)
       VALUES ($1, $2, $3), ($1, $4, 'member')`,
      [conversation.id, currentUserId, 'owner', targetUserId]
    );

    await client.query('COMMIT');

    res.status(201).json({
      success: true,
      data: conversation
    });
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    next(error);
  } finally {
    client.release();
  }
};

const getConversations = async (req, res, next) => {
  try {
    const userId = req.user.id;
    const page = parsePage(req.query.page, 1);
    const limit = Math.min(parsePage(req.query.limit, 20), 100);
    const offset = (page - 1) * limit;

    const countRs = await query(
      `SELECT COUNT(*)::int AS total
       FROM conversation_members cm
       JOIN users self ON self.id = cm.user_id
       WHERE cm.user_id = $1
         AND COALESCE(self.account_status, 'active') = 'active'`,
      [userId]
    );
    const total = countRs.rows[0]?.total || 0;

    const rs = await query(
      `SELECT
         c.id,
         c.conversation_type,
         c.title,
         c.last_message_at,
         c.created_at,
         lm.id AS last_message_id,
         lm.content AS last_message_content,
         lm.sender_id AS last_message_sender_id,
         lm.created_at AS last_message_created_at,
         other.id AS other_user_id,
         other.username AS other_username,
         other.display_name AS other_display_name,
         other.avatar_url AS other_avatar_url,
         COALESCE(unread.unread_count, 0) AS unread_count
       FROM conversation_members cm
       JOIN conversations c ON c.id = cm.conversation_id
       LEFT JOIN messages lm ON lm.id = c.last_message_id
       LEFT JOIN LATERAL (
          SELECT u.id, u.username, u.display_name, u.avatar_url
          FROM conversation_members cm2
          JOIN users u ON u.id = cm2.user_id
          WHERE cm2.conversation_id = c.id
            AND cm2.user_id <> $1
            AND COALESCE(u.account_status, 'active') = 'active'
          LIMIT 1
        ) other ON true
       LEFT JOIN LATERAL (
         SELECT COUNT(*)::int AS unread_count
         FROM messages m
         WHERE m.conversation_id = c.id
           AND m.sender_id <> $1
           AND (cm.last_read_at IS NULL OR m.created_at > cm.last_read_at)
       ) unread ON true
        WHERE cm.user_id = $1
          AND other.id IS NOT NULL
        ORDER BY COALESCE(c.last_message_at, c.created_at) DESC
        LIMIT $2 OFFSET $3`,
      [userId, limit, offset]
    );

    res.status(200).json({
      success: true,
      data: rs.rows,
      pagination: {
        total,
        page,
        limit,
        totalPages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    next(error);
  }
};

const getMessages = async (req, res, next) => {
  try {
    const userId = req.user.id;
    const conversationId = parseInt(req.params.id, 10);
    const page = parsePage(req.query.page, 1);
    const limit = Math.min(parsePage(req.query.limit, 30), 100);
    const offset = (page - 1) * limit;

    if (!Number.isFinite(conversationId) || conversationId <= 0) {
      throw new BadRequestError('Invalid conversation id');
    }

    await ensureMembership(conversationId, userId);

    const countRs = await query(
      `SELECT COUNT(*)::int AS total
       FROM messages
       WHERE conversation_id = $1`,
      [conversationId]
    );
    const total = countRs.rows[0]?.total || 0;

    const rs = await query(
      `SELECT
         m.id,
         m.conversation_id,
         m.sender_id,
         m.content,
         m.message_type,
         m.metadata,
         m.reply_to_message_id,
         m.is_deleted,
         m.created_at,
         m.updated_at,
         u.username AS sender_username,
         u.display_name AS sender_display_name,
         u.avatar_url AS sender_avatar_url
       FROM messages m
        JOIN users u ON u.id = m.sender_id
        WHERE m.conversation_id = $1
          AND COALESCE(u.account_status, 'active') = 'active'
        ORDER BY m.created_at DESC
       LIMIT $2 OFFSET $3`,
      [conversationId, limit, offset]
    );

    res.status(200).json({
      success: true,
      data: rs.rows,
      pagination: {
        total,
        page,
        limit,
        totalPages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    next(error);
  }
};

const sendMessage = async (req, res, next) => {
  try {
    const userId = req.user.id;
    const conversationId = parseInt(req.params.id, 10);
    const content = String(req.body.content || '').trim();
    const messageType = req.body.message_type || 'text';
    const metadata = req.body.metadata || null;
    const replyToMessageId = req.body.reply_to_message_id || null;

    if (!Number.isFinite(conversationId) || conversationId <= 0) {
      throw new BadRequestError('Invalid conversation id');
    }
    if (!content) {
      throw new BadRequestError('content is required');
    }

    await ensureMembership(conversationId, userId);

    const inserted = await query(
      `INSERT INTO messages
         (conversation_id, sender_id, content, message_type, metadata, reply_to_message_id)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING id, conversation_id, sender_id, content, message_type, metadata, reply_to_message_id, is_deleted, created_at, updated_at`,
      [conversationId, userId, content, messageType, metadata, replyToMessageId]
    );
    const message = inserted.rows[0];

    await query(
      `UPDATE conversations
       SET last_message_id = $1,
           last_message_at = $2,
           updated_at = NOW()
       WHERE id = $3`,
      [message.id, message.created_at, conversationId]
    );

    const senderRs = await query(
      `SELECT username, display_name, avatar_url
       FROM users
       WHERE id = $1
         AND COALESCE(account_status, 'active') = 'active'`,
      [userId]
    );

    res.status(201).json({
      success: true,
      data: {
        ...message,
        sender_username: senderRs.rows[0]?.username || null,
        sender_display_name: senderRs.rows[0]?.display_name || null,
        sender_avatar_url: senderRs.rows[0]?.avatar_url || null
      }
    });
  } catch (error) {
    next(error);
  }
};

const markConversationRead = async (req, res, next) => {
  try {
    const userId = req.user.id;
    const conversationId = parseInt(req.params.id, 10);

    if (!Number.isFinite(conversationId) || conversationId <= 0) {
      throw new BadRequestError('Invalid conversation id');
    }

    await ensureMembership(conversationId, userId);

    const rs = await query(
      `UPDATE conversation_members
       SET last_read_at = NOW()
       WHERE conversation_id = $1 AND user_id = $2
       RETURNING conversation_id, user_id, last_read_at`,
      [conversationId, userId]
    );

    res.status(200).json({
      success: true,
      data: rs.rows[0]
    });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  createOrGetDirectConversation,
  getConversations,
  getMessages,
  sendMessage,
  markConversationRead
};
