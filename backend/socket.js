const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');
const { query } = require('./config/db-connection');

const validateSocketToken = (socket, next) => {
  try {
    const authHeader = socket.handshake.headers?.authorization;
    const bearerToken = authHeader && authHeader.startsWith('Bearer ')
      ? authHeader.slice(7)
      : null;
    const token =
      socket.handshake.auth?.token ||
      socket.handshake.query?.token ||
      bearerToken;

    if (!token) {
      return next(new Error('Unauthorized'));
    }

    const decoded = jwt.verify(token, process.env.JWT_SECRET, {
      algorithms: ['HS256'],
      issuer: 'soul-api',
      audience: 'soul-app'
    });

    socket.user = {
      id: decoded.userId,
      username: decoded.username,
      role: decoded.role || 'user'
    };
    return next();
  } catch (_e) {
    return next(new Error('Unauthorized'));
  }
};

const isConversationMember = async (conversationId, userId) => {
  const rs = await query(
    `SELECT 1
     FROM conversation_members
     WHERE conversation_id = $1 AND user_id = $2`,
    [conversationId, userId]
  );
  return rs.rows.length > 0;
};

const initSocket = (server) => {
  const io = new Server(server, {
    cors: {
      origin: process.env.CLIENT_URL || '*',
      methods: ['GET', 'POST'],
      credentials: true
    }
  });

  io.use(validateSocketToken);

  io.on('connection', (socket) => {
    const userId = socket.user.id;
    socket.join(`user:${userId}`);

    socket.on('conversation:join', async (payload = {}, ack) => {
      try {
        const conversationId = parseInt(payload.conversationId, 10);
        if (!Number.isFinite(conversationId) || conversationId <= 0) {
          throw new Error('Invalid conversationId');
        }
        const member = await isConversationMember(conversationId, userId);
        if (!member) throw new Error('Forbidden');
        socket.join(`conv:${conversationId}`);
        if (typeof ack === 'function') ack({ success: true, conversationId });
      } catch (error) {
        if (typeof ack === 'function') ack({ success: false, error: error.message });
      }
    });

    socket.on('conversation:leave', (payload = {}, ack) => {
      const conversationId = parseInt(payload.conversationId, 10);
      if (Number.isFinite(conversationId) && conversationId > 0) {
        socket.leave(`conv:${conversationId}`);
      }
      if (typeof ack === 'function') ack({ success: true });
    });

    socket.on('message:send', async (payload = {}, ack) => {
      try {
        const conversationId = parseInt(payload.conversationId, 10);
        const content = String(payload.content || '').trim();
        const messageType = payload.message_type || 'text';
        const metadata = payload.metadata || null;
        const replyToMessageId = payload.reply_to_message_id || null;

        if (!Number.isFinite(conversationId) || conversationId <= 0) {
          throw new Error('Invalid conversationId');
        }
        if (!content) {
          throw new Error('content is required');
        }

        const member = await isConversationMember(conversationId, userId);
        if (!member) throw new Error('Forbidden');

        const insertRs = await query(
          `INSERT INTO messages
            (conversation_id, sender_id, content, message_type, metadata, reply_to_message_id)
           VALUES ($1, $2, $3, $4, $5, $6)
           RETURNING id, conversation_id, sender_id, content, message_type, metadata, reply_to_message_id, is_deleted, created_at, updated_at`,
          [conversationId, userId, content, messageType, metadata, replyToMessageId]
        );
        const message = insertRs.rows[0];

        await query(
          `UPDATE conversations
           SET last_message_id = $1,
               last_message_at = $2,
               updated_at = NOW()
           WHERE id = $3`,
          [message.id, message.created_at, conversationId]
        );

        const senderRs = await query(
          `SELECT username, display_name, avatar_url FROM users WHERE id = $1`,
          [userId]
        );
        const sender = senderRs.rows[0] || {};

        const eventData = {
          ...message,
          sender_username: sender.username || null,
          sender_display_name: sender.display_name || null,
          sender_avatar_url: sender.avatar_url || null
        };

        io.to(`conv:${conversationId}`).emit('message:new', eventData);

        const membersRs = await query(
          `SELECT user_id FROM conversation_members WHERE conversation_id = $1`,
          [conversationId]
        );
        membersRs.rows.forEach((row) => {
          io.to(`user:${row.user_id}`).emit('conversation:updated', {
            conversationId,
            lastMessage: eventData
          });
        });

        if (typeof ack === 'function') ack({ success: true, data: eventData });
      } catch (error) {
        if (typeof ack === 'function') ack({ success: false, error: error.message });
      }
    });

    socket.on('conversation:read', async (payload = {}, ack) => {
      try {
        const conversationId = parseInt(payload.conversationId, 10);
        if (!Number.isFinite(conversationId) || conversationId <= 0) {
          throw new Error('Invalid conversationId');
        }
        const member = await isConversationMember(conversationId, userId);
        if (!member) throw new Error('Forbidden');

        const updateRs = await query(
          `UPDATE conversation_members
           SET last_read_at = NOW()
           WHERE conversation_id = $1 AND user_id = $2
           RETURNING last_read_at`,
          [conversationId, userId]
        );
        const readAt = updateRs.rows[0]?.last_read_at || new Date().toISOString();

        io.to(`conv:${conversationId}`).emit('conversation:read', {
          conversationId,
          userId,
          readAt
        });
        if (typeof ack === 'function') ack({ success: true, readAt });
      } catch (error) {
        if (typeof ack === 'function') ack({ success: false, error: error.message });
      }
    });
  });

  return io;
};

module.exports = { initSocket };

