const { query, getClient } = require('../config/db-connection');
const { 
  NotFoundError, 
  BadRequestError,
  ForbiddenError,
  DatabaseError
} = require('../utils/errors');
const { body } = require('express-validator');

// Danh sách các loại item được phép
const ALLOWED_ITEM_TYPES = ['book', 'movie', 'music', 'artist', 'game', 'other'];

const itemsController = {
  // Validate item input
  validateItem: [
    body('type')
      .isIn(ALLOWED_ITEM_TYPES)
      .withMessage(`Loại item không hợp lệ. Chấp nhận: ${ALLOWED_ITEM_TYPES.join(', ')}`),
    body('title')
      .trim()
      .isLength({ min: 1, max: 255 })
      .withMessage('Tiêu đề phải từ 1-255 ký tự'),
    body('description').optional().trim(),
    body('cover_image_url').optional().isURL().withMessage('URL ảnh bìa không hợp lệ'),
    body('external_id').optional().isString(),
    body('metadata').optional().isObject().withMessage('Metadata phải là đối tượng JSON')
  ],

  // Lấy danh sách items
  async getAllItems(req, res, next) {
  try {
    console.log('=== BẮT ĐẦU XỬ LÝ GET ALL ITEMS ===');
    
    const { 
      page = 1, 
      limit = 10, 
      type, 
      search,
      sort_by = 'created_at',
      sort_order = 'desc'
    } = req.query;

    const offset = (page - 1) * limit;
    const queryParams = [];
    const whereClauses = ['1=1'];

    // Xây dựng điều kiện WHERE
    if (type) {
      queryParams.push(type);
      whereClauses.push(`i.type = $${queryParams.length}`);
    }

    if (search) {
      queryParams.push(`%${search}%`);
      whereClauses.push(`(i.title ILIKE $${queryParams.length} OR i.description ILIKE $${queryParams.length})`);
    }

    // Xây dựng câu query
    const queryText = `
      SELECT 
        i.*,
        u.username as creator_username,
        u.avatar_url as creator_avatar
      FROM items i
      LEFT JOIN users u ON i.created_by = u.id
      WHERE ${whereClauses.join(' AND ')}
      ORDER BY i.${sort_by} ${sort_order.toUpperCase() === 'ASC' ? 'ASC' : 'DESC'}
      LIMIT ${parseInt(limit)} OFFSET ${offset}
    `;

    console.log('=== THÔNG TIN TRUY VẤN ===');
    console.log('Query:', queryText);
    console.log('Params:', queryParams);

    // Thực hiện truy vấn
    const result = await query(queryText, queryParams);
    
    // Lấy tổng số bản ghi
    const countQuery = `
      SELECT COUNT(*) 
      FROM items i
      WHERE ${whereClauses.join(' AND ')}
    `;
    
    const countResult = await query(countQuery, queryParams);
    const total = parseInt(countResult.rows[0]?.count || 0);

    if (result.rows.length === 0) {
      console.log('Không tìm thấy items nào');
      return res.json({
        success: true,
        data: [],
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total: 0,
          total_pages: 0
        }
      });
    }

    res.json({
      success: true,
      data: result.rows,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total,
        total_pages: Math.ceil(total / limit)
      }
    });
  } catch (error) {
    console.error('=== LỖI TRONG GETALLITEMS ===');
    console.error('Thông báo lỗi:', error.message);
    console.error('Stack trace:', error.stack);
    next(new DatabaseError('Lỗi khi lấy danh sách items: ' + error.message));
  }
},

  // Lấy chi tiết item
  async getItemById(req, res, next) {
    try {
      const { id } = req.params;
      
      const result = await query(
        `SELECT i.*, 
                u.username as creator_username,
                u.avatar_url as creator_avatar
         FROM items i
         LEFT JOIN users u ON i.created_by = u.id
         WHERE i.id = $1`,
        [id]
      );

      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy item');
      }

      res.json({
        success: true,
        data: result.rows[0]
      });
    } catch (error) {
      next(error);
    }
  },

  // Tạo mới item
  async createItem(req, res, next) {
    const client = await getClient();
    
    try {
      const { 
        type, 
        title, 
        description = '', 
        cover_image_url, 
        external_id, 
        metadata = {} 
      } = req.body;

      // Kiểm tra quyền tạo item
      if (!req.user) {
        throw new ForbiddenError('Bạn cần đăng nhập để tạo item mới');
      }

      await client.query('BEGIN');

      // Kiểm tra xem external_id đã tồn tại chưa (nếu có)
      if (external_id) {
        const existingItem = await client.query(
          'SELECT id FROM items WHERE external_id = $1',
          [external_id]
        );
        
        if (existingItem.rows.length > 0) {
          throw new BadRequestError('Item với external_id này đã tồn tại');
        }
      }

      // Thêm item mới
      const result = await client.query(
        `INSERT INTO items (
          type, 
          title, 
          description, 
          cover_image_url, 
          external_id, 
          metadata, 
          created_by
        ) VALUES ($1, $2, $3, $4, $5, $6, $7)
        RETURNING *`,
        [
          type,
          title,
          description,
          cover_image_url,
          external_id,
          metadata,
          req.user.id
        ]
      );

      await client.query('COMMIT');
      
      const newItem = result.rows[0];
      
      res.status(201).json({
        success: true,
        message: 'Tạo item thành công',
        data: newItem
      });
    } catch (error) {
      await client.query('ROLLBACK');
      
      if (error.code === '23505') { // Unique violation
        next(new BadRequestError('Item với tiêu đề này đã tồn tại'));
      } else {
        next(error);
      }
    } finally {
      client.release();
    }
  },

  // Cập nhật item
  async updateItem(req, res, next) {
    const client = await getClient();
    
    try {
      const { id } = req.params;
      const { 
        type, 
        title, 
        description, 
        cover_image_url, 
        external_id, 
        metadata 
      } = req.body;

      await client.query('BEGIN');

      // Kiểm tra item có tồn tại không
      const itemResult = await client.query(
        'SELECT * FROM items WHERE id = $1',
        [id]
      );

      if (itemResult.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy item');
      }

      const item = itemResult.rows[0];

      // Kiểm tra quyền sở hữu (chỉ người tạo hoặc admin mới được sửa)
      if (item.created_by !== req.user.id && req.user.role !== 'admin') {
        throw new ForbiddenError('Bạn không có quyền cập nhật item này');
      }

      // Kiểm tra external_id trùng lặp (nếu có)
      if (external_id && external_id !== item.external_id) {
        const existingItem = await client.query(
          'SELECT id FROM items WHERE external_id = $1 AND id != $2',
          [external_id, id]
        );
        
        if (existingItem.rows.length > 0) {
          throw new BadRequestError('Item với external_id này đã tồn tại');
        }
      }

      // Cập nhật item
      const result = await client.query(
        `UPDATE items 
         SET type = COALESCE($1, type),
             title = COALESCE($2, title),
             description = COALESCE($3, description),
             cover_image_url = COALESCE($4, cover_image_url),
             external_id = COALESCE($5, external_id),
             metadata = COALESCE($6, metadata),
             updated_at = NOW()
         WHERE id = $7
         RETURNING *`,
        [
          type || null,
          title || null,
          description || null,
          cover_image_url || null,
          external_id || null,
          metadata || null,
          id
        ]
      );

      await client.query('COMMIT');
      
      res.json({
        success: true,
        message: 'Cập nhật item thành công',
        data: result.rows[0]
      });
    } catch (error) {
      await client.query('ROLLBACK');
      next(error);
    } finally {
      client.release();
    }
  },

  // Xóa item
  async deleteItem(req, res, next) {
    const client = await getClient();
    
    try {
      const { id } = req.params;

      await client.query('BEGIN');

      // Kiểm tra item có tồn tại không
      const itemResult = await client.query(
        'SELECT created_by FROM items WHERE id = $1',
        [id]
      );

      if (itemResult.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy item');
      }

      // Kiểm tra quyền sở hữu (chỉ người tạo hoặc admin mới được xóa)
      if (itemResult.rows[0].created_by !== req.user.id && req.user.role !== 'admin') {
        throw new ForbiddenError('Bạn không có quyền xóa item này');
      }

      // Xóa các bản ghi liên quan trong collection_items
      await client.query(
        'DELETE FROM collection_items WHERE item_id = $1',
        [id]
      );

      // Xóa item
      await client.query(
        'DELETE FROM items WHERE id = $1',
        [id]
      );

      await client.query('COMMIT');
      
      res.json({
        success: true,
        message: 'Xóa item thành công'
      });
    } catch (error) {
      await client.query('ROLLBACK');
      next(error);
    } finally {
      client.release();
    }
  },

  // Tìm kiếm items
  async searchItems(req, res, next) {
    try {
      const { q, type, page = 1, limit = 10 } = req.query;
      
      if (!q) {
        throw new BadRequestError('Vui lòng nhập từ khóa tìm kiếm');
      }

      const offset = (page - 1) * limit;
      const searchTerm = `%${q}%`;
      const queryParams = [searchTerm];
      const whereClauses = ['(title ILIKE $1 OR description ILIKE $1)'];

      // Thêm điều kiện lọc theo type nếu có
      if (type && ALLOWED_ITEM_TYPES.includes(type)) {
        queryParams.push(type);
        whereClauses.push(`type = $${queryParams.length}`);
      }

      // Tìm kiếm items
      let queryText = `
        SELECT * FROM items 
        WHERE ${whereClauses.join(' AND ')}
        ORDER BY 
          CASE 
            WHEN title ILIKE $1 THEN 1
            WHEN description ILIKE $1 THEN 2
            ELSE 3
          END,
          created_at DESC
        LIMIT $${queryParams.length + 1} OFFSET $${queryParams.length + 2}
      `;

      queryParams.push(limit, offset);

      const result = await query(queryText, queryParams);

      // Đếm tổng số kết quả
      const countQuery = `
        SELECT COUNT(*) 
        FROM items 
        WHERE ${whereClauses.join(' AND ')}
      `;
      
      const countResult = await query(countQuery, queryParams.slice(0, -2));
      const total = parseInt(countResult.rows[0].count);

      res.json({
        success: true,
        data: result.rows,
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total,
          total_pages: Math.ceil(total / limit)
        }
      });
    } catch (error) {
      next(error);
    }
  }
};

module.exports = itemsController;
