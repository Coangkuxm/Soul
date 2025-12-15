const { query } = require('../config/db-connection');
const { DatabaseError, NotFoundError, ForbiddenError } = require('../utils/errors');

const collectionItemsController = {
  // Thêm item vào collection
  async addItemToCollection(req, res, next) {
    try {
      const { collection_id, item_id } = req.body;
      const user_id = req.user.id;

      // Kiểm tra quyền sở hữu collection
      const collectionCheck = await query(
        'SELECT created_by FROM collections WHERE id = $1',
        [collection_id]
      );

      if (collectionCheck.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy collection');
      }

      if (collectionCheck.rows[0].created_by !== user_id) {
        throw new ForbiddenError('Bạn không có quyền thêm item vào collection này');
      }

      // Kiểm tra item có tồn tại không
      const itemCheck = await query('SELECT 1 FROM items WHERE id = $1', [item_id]);
      if (itemCheck.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy item');
      }

      // Kiểm tra item đã có trong collection chưa
      const exists = await query(
        'SELECT 1 FROM collection_items WHERE collection_id = $1 AND item_id = $2',
        [collection_id, item_id]
      );

      if (exists.rows.length > 0) {
        return res.status(200).json({
          success: true,
          message: 'Item đã có trong collection',
          data: { collection_id, item_id }
        });
      }

      // Thêm item vào collection
      const result = await query(
        'INSERT INTO collection_items (collection_id, item_id, created_by) VALUES ($1, $2, $3) RETURNING *',
        [collection_id, item_id, user_id]
      );

      res.status(201).json({
        success: true,
        message: 'Đã thêm item vào collection',
        data: result.rows[0]
      });
    } catch (error) {
      next(new DatabaseError('Lỗi khi thêm item vào collection: ' + error.message));
    }
  },

  // Xóa item khỏi collection
  async removeItemFromCollection(req, res, next) {
    try {
      const { collection_id, item_id } = req.params;
      const user_id = req.user.id;

      // Kiểm tra quyền sở hữu collection
      const collectionCheck = await query(
        'SELECT created_by FROM collections WHERE id = $1',
        [collection_id]
      );

      if (collectionCheck.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy collection');
      }

      if (collectionCheck.rows[0].created_by !== user_id) {
        throw new ForbiddenError('Bạn không có quyền xóa item khỏi collection này');
      }

      // Xóa item khỏi collection
      const result = await query(
        'DELETE FROM collection_items WHERE collection_id = $1 AND item_id = $2 RETURNING *',
        [collection_id, item_id]
      );

      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy item trong collection');
      }

      res.json({
        success: true,
        message: 'Đã xóa item khỏi collection',
        data: result.rows[0]
      });
    } catch (error) {
      next(new DatabaseError('Lỗi khi xóa item khỏi collection: ' + error.message));
    }
  },

  // Lấy danh sách items trong collection
  async getItemsInCollection(req, res, next) {
    try {
      const { collection_id } = req.params;
      const { page = 1, limit = 10 } = req.query;
      const offset = (page - 1) * limit;

      // Kiểm tra collection có tồn tại không
      const collectionCheck = await query('SELECT * FROM collections WHERE id = $1', [collection_id]);
      if (collectionCheck.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy collection');
      }

      // Lấy tổng số items
      const countResult = await query(
        'SELECT COUNT(*) FROM collection_items WHERE collection_id = $1',
        [collection_id]
      );
      const total = parseInt(countResult.rows[0].count);

      // Lấy danh sách items
      const itemsResult = await query(
        `SELECT i.*, ci.created_at as added_at 
         FROM items i
         JOIN collection_items ci ON i.id = ci.item_id
         WHERE ci.collection_id = $1
         ORDER BY ci.created_at DESC
         LIMIT $2 OFFSET $3`,
        [collection_id, limit, offset]
      );

      res.json({
        success: true,
        data: itemsResult.rows,
        pagination: {
          page: parseInt(page),
          limit: parseInt(limit),
          total,
          total_pages: Math.ceil(total / limit)
        }
      });
    } catch (error) {
      next(new DatabaseError('Lỗi khi lấy danh sách items trong collection: ' + error.message));
    }
  }
};

module.exports = collectionItemsController;
