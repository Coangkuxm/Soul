const db = require('../config/db-connection');
const {
  BadRequestError,
  ConflictError,
  ForbiddenError,
  NotFoundError
} = require('../utils/errors');

const ALLOWED_TARGET_TYPES = ['user', 'collection_item'];
const ALLOWED_REPORT_STATUSES = ['pending', 'reviewed', 'dismissed', 'actioned'];

function parsePositiveInt(value, fieldName) {
  const parsed = parseInt(value, 10);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new BadRequestError(`${fieldName} không hợp lệ`);
  }
  return parsed;
}

function normalizeTargetType(value) {
  if (!ALLOWED_TARGET_TYPES.includes(value)) {
    throw new BadRequestError('targetType không hợp lệ');
  }
  return value;
}

async function ensureTargetExists(targetType, targetId) {
  if (targetType === 'user') {
    const result = await db.query(
      `SELECT id, username, display_name, role, account_status
       FROM users
       WHERE id = $1`,
      [targetId]
    );
    if (result.rows.length === 0) {
      throw new NotFoundError('Không tìm thấy tài khoản bị báo cáo');
    }
    return result.rows[0];
  }

  const result = await db.query(
    `SELECT
        ci.id,
        ci.moderation_status,
        ci.collection_id,
        ci.item_id,
        i.title,
        c.name AS collection_name,
        c.owner_id
     FROM collection_items ci
     LEFT JOIN items i ON i.id = ci.item_id
     LEFT JOIN collections c ON c.id = ci.collection_id
     WHERE ci.id = $1`,
    [targetId]
  );

  if (result.rows.length === 0) {
    throw new NotFoundError('Không tìm thấy bài viết bị báo cáo');
  }

  return result.rows[0];
}

class ReportsController {
  async createReport(req, res, next) {
    try {
      const reporterId = req.user.id;
      const targetType = normalizeTargetType(req.body.targetType);
      const targetId = parsePositiveInt(req.body.targetId, 'targetId');
      const reasonCode = String(req.body.reasonCode || '').trim();
      const reasonDetail = req.body.reasonDetail ? String(req.body.reasonDetail).trim() : null;

      if (!reasonCode) {
        throw new BadRequestError('reasonCode là bắt buộc');
      }

      const target = await ensureTargetExists(targetType, targetId);

      if (targetType === 'user' && targetId === reporterId) {
        throw new BadRequestError('Bạn không thể tự báo cáo chính mình');
      }

      try {
        const insertResult = await db.query(
          `INSERT INTO reports (
             reporter_id, target_type, target_id, reason_code, reason_detail
           )
           VALUES ($1, $2, $3, $4, $5)
           RETURNING *`,
          [reporterId, targetType, targetId, reasonCode, reasonDetail]
        );

        res.status(201).json({
          success: true,
          message: 'Đã gửi báo cáo',
          data: insertResult.rows[0]
        });
      } catch (error) {
        if (error.code === '23505') {
          throw new ConflictError('Bạn đã gửi báo cáo đang chờ xử lý cho đối tượng này');
        }
        throw error;
      }
    } catch (error) {
      next(error);
    }
  }

  async listReports(req, res, next) {
    try {
      const targetType = req.query.targetType ? normalizeTargetType(req.query.targetType) : null;
      const status = req.query.status ? String(req.query.status).trim() : null;
      const limit = Math.min(parsePositiveInt(req.query.limit || 20, 'limit'), 100);
      const page = parsePositiveInt(req.query.page || 1, 'page');
      const offset = (page - 1) * limit;

      if (status && !ALLOWED_REPORT_STATUSES.includes(status)) {
        throw new BadRequestError('status không hợp lệ');
      }

      const whereClauses = [];
      const params = [];

      if (targetType) {
        params.push(targetType);
        whereClauses.push(`r.target_type = $${params.length}`);
      }

      if (status) {
        params.push(status);
        whereClauses.push(`r.status = $${params.length}`);
      }

      const whereSql = whereClauses.length ? `WHERE ${whereClauses.join(' AND ')}` : '';

      const listQuery = `
        SELECT
          r.*,
          reporter.username AS reporter_username,
          reviewer.username AS reviewer_username
        FROM reports r
        JOIN users reporter ON reporter.id = r.reporter_id
        LEFT JOIN users reviewer ON reviewer.id = r.reviewed_by
        ${whereSql}
        ORDER BY r.created_at DESC
        LIMIT $${params.length + 1} OFFSET $${params.length + 2}
      `;

      const countQuery = `SELECT COUNT(*) FROM reports r ${whereSql}`;
      const [listResult, countResult] = await Promise.all([
        db.query(listQuery, [...params, limit, offset]),
        db.query(countQuery, params)
      ]);

      res.json({
        success: true,
        data: listResult.rows,
        pagination: {
          page,
          limit,
          total: parseInt(countResult.rows[0].count, 10)
        }
      });
    } catch (error) {
      next(error);
    }
  }

  async listReportedPosts(req, res, next) {
    try {
      const status = req.query.status ? String(req.query.status).trim() : null;

      if (status && !ALLOWED_REPORT_STATUSES.includes(status)) {
        throw new BadRequestError('status không hợp lệ');
      }

      const params = ['collection_item'];
      let statusSql = '';
      if (status) {
        params.push(status);
        statusSql = ` AND r.status = $2`;
      }

      const result = await db.query(
        `SELECT
           r.target_id AS collection_item_id,
           COUNT(*)::int AS report_count,
           MAX(r.created_at) AS latest_report_at,
           ARRAY_REMOVE(ARRAY_AGG(DISTINCT r.reason_code), NULL) AS reason_codes,
           ci.moderation_status,
           ci.locked_at,
           ci.locked_reason,
           ci.collection_id,
           ci.item_id,
           i.title AS item_title,
           c.name AS collection_name,
           c.owner_id,
           u.username AS owner_username
         FROM reports r
         JOIN collection_items ci ON ci.id = r.target_id
         LEFT JOIN items i ON i.id = ci.item_id
         LEFT JOIN collections c ON c.id = ci.collection_id
         LEFT JOIN users u ON u.id = c.owner_id
         WHERE r.target_type = $1${statusSql}
         GROUP BY
           r.target_id,
           ci.moderation_status,
           ci.locked_at,
           ci.locked_reason,
           ci.collection_id,
           ci.item_id,
           i.title,
           c.name,
           c.owner_id,
           u.username
         ORDER BY latest_report_at DESC, report_count DESC`
        ,
        params
      );

      res.json({
        success: true,
        data: result.rows
      });
    } catch (error) {
      next(error);
    }
  }

  async listReportedUsers(req, res, next) {
    try {
      const status = req.query.status ? String(req.query.status).trim() : null;

      if (status && !ALLOWED_REPORT_STATUSES.includes(status)) {
        throw new BadRequestError('status không hợp lệ');
      }

      const params = ['user'];
      let statusSql = '';
      if (status) {
        params.push(status);
        statusSql = ` AND r.status = $2`;
      }

      const result = await db.query(
        `SELECT
           r.target_id AS user_id,
           COUNT(*)::int AS report_count,
           MAX(r.created_at) AS latest_report_at,
           ARRAY_REMOVE(ARRAY_AGG(DISTINCT r.reason_code), NULL) AS reason_codes,
           u.username,
           u.display_name,
           u.role,
           u.account_status,
           u.locked_at,
           u.locked_reason
         FROM reports r
         JOIN users u ON u.id = r.target_id
         WHERE r.target_type = $1${statusSql}
         GROUP BY
           r.target_id,
           u.username,
           u.display_name,
           u.role,
           u.account_status,
           u.locked_at,
           u.locked_reason
         ORDER BY latest_report_at DESC, report_count DESC`,
        params
      );

      res.json({
        success: true,
        data: result.rows
      });
    } catch (error) {
      next(error);
    }
  }

  async updateReportStatus(req, res, next) {
    try {
      const reportId = parsePositiveInt(req.params.id, 'reportId');
      const status = String(req.body.status || '').trim();
      const resolutionNote = req.body.resolutionNote ? String(req.body.resolutionNote).trim() : null;

      if (!ALLOWED_REPORT_STATUSES.includes(status) || status === 'pending') {
        throw new BadRequestError('status cập nhật không hợp lệ');
      }

      const result = await db.query(
        `UPDATE reports
         SET status = $1,
             resolution_note = $2,
             reviewed_by = $3,
             reviewed_at = NOW(),
             updated_at = NOW()
         WHERE id = $4
         RETURNING *`,
        [status, resolutionNote, req.user.id, reportId]
      );

      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy báo cáo');
      }

      res.json({
        success: true,
        message: 'Đã cập nhật trạng thái báo cáo',
        data: result.rows[0]
      });
    } catch (error) {
      next(error);
    }
  }

  async lockPost(req, res, next) {
    try {
      const collectionItemId = parsePositiveInt(req.params.id, 'collectionItemId');
      const reason = req.body.reason ? String(req.body.reason).trim() : null;

      const result = await db.query(
        `UPDATE collection_items
         SET moderation_status = 'locked',
             locked_at = NOW(),
             locked_reason = $2,
             locked_by = $3
         WHERE id = $1
         RETURNING *`,
        [collectionItemId, reason, req.user.id]
      );

      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy bài viết');
      }

      await db.query(
        `UPDATE reports
         SET status = 'actioned',
             reviewed_by = $1,
             reviewed_at = NOW(),
             resolution_note = COALESCE($2, resolution_note),
             updated_at = NOW()
         WHERE target_type = 'collection_item'
           AND target_id = $3
           AND status = 'pending'`,
        [req.user.id, reason, collectionItemId]
      );

      res.json({
        success: true,
        message: 'Đã khóa bài viết',
        data: result.rows[0]
      });
    } catch (error) {
      next(error);
    }
  }

  async unlockPost(req, res, next) {
    try {
      const collectionItemId = parsePositiveInt(req.params.id, 'collectionItemId');

      const result = await db.query(
        `UPDATE collection_items
         SET moderation_status = 'active',
             locked_at = NULL,
             locked_reason = NULL,
             locked_by = NULL
         WHERE id = $1
         RETURNING *`,
        [collectionItemId]
      );

      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy bài viết');
      }

      res.json({
        success: true,
        message: 'Đã mở khóa bài viết',
        data: result.rows[0]
      });
    } catch (error) {
      next(error);
    }
  }

  async lockUser(req, res, next) {
    try {
      const userId = parsePositiveInt(req.params.id, 'userId');
      const reason = req.body.reason ? String(req.body.reason).trim() : null;

      if (userId === req.user.id) {
        throw new ForbiddenError('Bạn không thể tự khóa tài khoản của chính mình');
      }

      const result = await db.query(
        `UPDATE users
         SET account_status = 'locked',
             locked_at = NOW(),
             locked_reason = $2,
             locked_by = $3
         WHERE id = $1
         RETURNING id, username, role, account_status, locked_at, locked_reason, locked_by`,
        [userId, reason, req.user.id]
      );

      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy tài khoản');
      }

      await db.query(
        `UPDATE reports
         SET status = 'actioned',
             reviewed_by = $1,
             reviewed_at = NOW(),
             resolution_note = COALESCE($2, resolution_note),
             updated_at = NOW()
         WHERE target_type = 'user'
           AND target_id = $3
           AND status = 'pending'`,
        [req.user.id, reason, userId]
      );

      res.json({
        success: true,
        message: 'Đã khóa tài khoản',
        data: result.rows[0]
      });
    } catch (error) {
      next(error);
    }
  }

  async unlockUser(req, res, next) {
    try {
      const userId = parsePositiveInt(req.params.id, 'userId');

      const result = await db.query(
        `UPDATE users
         SET account_status = 'active',
             locked_at = NULL,
             locked_reason = NULL,
             locked_by = NULL
         WHERE id = $1
         RETURNING id, username, role, account_status`,
        [userId]
      );

      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy tài khoản');
      }

      res.json({
        success: true,
        message: 'Đã mở khóa tài khoản',
        data: result.rows[0]
      });
    } catch (error) {
      next(error);
    }
  }
}

module.exports = new ReportsController();
