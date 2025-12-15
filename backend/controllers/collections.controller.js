const { query } = require('../config/db-connection');
const { NotFoundError, ForbiddenError } = require('../utils/errors');

const createCollection = async (req, res, next) => {
  
  try {
    const { name, description, cover_image_url, is_private, tags } = req.body;
    const owner_id = req.user.id;

    const result = await query(
      `INSERT INTO collections 
       (name, description, cover_image_url, is_private, owner_id)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING *`,
      [name, description, cover_image_url, is_private || false, owner_id]
    );

    if (tags && tags.length > 0) {
      await addCollectionTags(result.rows[0].id, tags);
    }

    await query(
      `INSERT INTO activities 
       (user_id, activity_type, collection_id)
       VALUES ($1, 'create_collection', $2)`,
      [owner_id, result.rows[0].id]
    );

    res.status(201).json({
      success: true,
      data: result.rows[0]
    });
  } catch (error) {
    next(error);
  }
};

const getCollections = async (req, res, next) => {
  
  console.log('=== BẮT ĐẦU XỬ LÝ GET COLLECTIONS ===');
  console.log('Thông tin request:', {
    method: req.method,
    url: req.originalUrl,
    query: req.query,
    user: req.user ? { id: req.user.id, role: req.user.role } : 'Không có thông tin người dùng'
  });

  try {
    // 1. Xử lý tham số
    const { 
      page = 1, 
      limit = 10, 
      user_id, 
      is_private,
      sort_by = 'created_at',
      sort_order = 'DESC'
    } = req.query;

    // Validate input
    const pageNum = Math.max(1, parseInt(page, 10) || 1);
    const limitNum = Math.min(parseInt(limit, 10) || 10, 100); // Giới hạn tối đa 100 items/trang
    const offset = (pageNum - 1) * limitNum;

    // Validate sort order và sort column
    const validSortOrders = ['ASC', 'DESC', 'asc', 'desc'];
    const validSortColumns = ['created_at', 'name', 'updated_at'];
    const orderBy = validSortColumns.includes(sort_by) ? sort_by : 'created_at';
    const order = validSortOrders.includes(sort_order.toUpperCase()) 
      ? sort_order.toUpperCase() 
      : 'DESC';

    // 2. Xây dựng câu truy vấn đếm tổng số bản ghi
    let countQuery = {
      text: `SELECT COUNT(*) FROM collections c WHERE 1=1`,
      values: []
    };

    // 3. Xây dựng câu truy vấn chính - đơn giản hóa để tránh treo
    let queryStr = `
      SELECT 
        c.*, 
        u.username, 
        u.avatar_url as owner_avatar
      FROM collections c
      JOIN users u ON c.owner_id = u.id
      WHERE 1=1
    `;

    const queryParams = [];
    
    // Thêm điều kiện lọc
    if (user_id) {
      const userId = parseInt(user_id, 10);
      if (!isNaN(userId)) {
        queryParams.push(userId);
        const paramIndex = queryParams.length;
        queryStr += ` AND c.owner_id = $${paramIndex}`;
        countQuery.text += ` AND c.owner_id = $${paramIndex}`;
        countQuery.values.push(userId);
      }
    }
    
    if (is_private !== undefined) {
      const isPrivate = is_private === 'true';
      queryParams.push(isPrivate);
      const paramIndex = queryParams.length;
      queryStr += ` AND c.is_private = $${paramIndex}`;
      countQuery.text += ` AND c.is_private = $${paramIndex}`;
      countQuery.values.push(isPrivate);
    }
    
    // Kiểm tra quyền xem collection private
    if (req.user) {
      queryParams.push(req.user.id);
      const paramIndex = queryParams.length;
      queryStr += ` AND (c.is_private = false OR c.owner_id = $${paramIndex})`;
      countQuery.text += ` AND (c.is_private = false OR c.owner_id = $${paramIndex})`;
      countQuery.values.push(req.user.id);
    } else {
      queryStr += ` AND c.is_private = false`;
      countQuery.text += ` AND c.is_private = false`;
    }
    
    // Thêm sắp xếp và phân trang
    queryStr += ` ORDER BY c.${orderBy} ${order}`;
    queryParams.push(limitNum, offset);
    queryStr += ` LIMIT $${queryParams.length - 1} OFFSET $${queryParams.length}`;

    console.log('Thực hiện truy vấn đếm:', countQuery);
    console.log('Thực hiện truy vấn dữ liệu:', {
      text: queryStr,
      values: queryParams
    });

    // Thực hiện truy vấn với timeout để tránh treo
    try {
      console.log('Bắt đầu thực hiện query...');
      
      // Thêm timeout 10 giây cho query
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Query timeout sau 10 giây')), 10000);
      });
      
      const [countResult, dataResult] = await Promise.race([
        Promise.all([
          query(countQuery),
          query({ text: queryStr, values: queryParams })
        ]),
        timeoutPromise
      ]);

      const totalItems = parseInt(countResult.rows[0].count);
      const totalPages = Math.ceil(totalItems / limitNum);

      console.log(`Kết quả: ${dataResult.rows.length} collections / ${totalItems} tổng cộng`);

      res.json({
        success: true,
        data: dataResult.rows,
        pagination: {
          currentPage: pageNum,
          itemsPerPage: limitNum,
          totalItems,
          totalPages,
          hasNextPage: pageNum < totalPages,
          hasPreviousPage: pageNum > 1
        }
      });
    } catch (queryError) {
      console.error('Lỗi truy vấn database:', queryError);
      res.status(500).json({
        success: false,
        error: 'Lỗi khi lấy dữ liệu collections'
      });
    }

  } catch (error) {
    console.error('Lỗi khi lấy danh sách collections:', {
      message: error.message,
      stack: error.stack,
      query: error.query
    });
    
    // Trả về lỗi chi tiết hơn cho client trong môi trường development
    if (process.env.NODE_ENV === 'development') {
      return res.status(500).json({
        success: false,
        error: error.message,
        stack: error.stack
      });
    }
    
    // Trong môi trường production chỉ trả về thông báo chung
    next(new Error('Đã xảy ra lỗi khi lấy danh sách collections. Vui lòng thử lại sau.'));
  }
};

const getCollectionById = async (req, res, next) => {
  try {
    const { id } = req.params;
    
    // Đơn giản hóa query để tránh treo - bỏ subquery
    const result = await query(
      `SELECT c.*, u.username, u.avatar_url as owner_avatar
       FROM collections c
       JOIN users u ON c.owner_id = u.id
       WHERE c.id = $1
       AND (c.is_private = false OR c.owner_id = $2)`,
      [id, req.user?.id]
    );

    if (result.rows.length === 0) {
      throw new NotFoundError('Không tìm thấy bộ sưu tập');
    }

    const items = await query(
      `SELECT i.*, ci.note, ci.rating, ci.added_at
       FROM items i
       JOIN collection_items ci ON i.id = ci.item_id
       WHERE ci.collection_id = $1
       ORDER BY ci.added_at DESC`,
      [id]
    );

    const tags = await query(
      `SELECT t.* FROM tags t
       JOIN collection_tags ct ON t.id = ct.tag_id
       WHERE ct.collection_id = $1`,
      [id]
    );

    if (!req.user || req.user.id !== result.rows[0].owner_id) {
      await query(
        'UPDATE collections SET view_count = view_count + 1 WHERE id = $1',
        [id]
      );
    }

    res.json({
      success: true,
      data: {
        ...result.rows[0],
        items: items.rows,
        tags: tags.rows
      }
    });
  } catch (error) {
    next(error);
  }
};

const updateCollection = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { name, description, cover_image_url, is_private, tags } = req.body;
    const user_id = req.user.id;

    await getCollectionIfOwner(id, user_id);
    
    const result = await query(
      `UPDATE collections 
       SET name = COALESCE($1, name),
           description = COALESCE($2, description),
           cover_image_url = COALESCE($3, cover_image_url),
           is_private = COALESCE($4, is_private),
           updated_at = NOW()
       WHERE id = $5
       RETURNING *`,
      [name, description, cover_image_url, is_private, id]
    );

    if (tags) {
      await query('DELETE FROM collection_tags WHERE collection_id = $1', [id]);
      if (tags.length > 0) {
        await addCollectionTags(id, tags);
      }
    }

    res.json({
      success: true,
      data: result.rows[0]
    });
  } catch (error) {
    next(error);
  }
};

const deleteCollection = async (req, res, next) => {
  try {
    const { id } = req.params;
    const user_id = req.user.id;

    await getCollectionIfOwner(id, user_id);
    
    await query('DELETE FROM collections WHERE id = $1', [id]);
    
    res.json({
      success: true,
      message: 'Đã xóa bộ sưu tập thành công'
    });
  } catch (error) {
    next(error);
  }
};

const addItemToCollection = async (req, res, next) => {
  try {
    const { id: collection_id } = req.params;
    const { item_id, note, rating } = req.body;
    const user_id = req.user.id;

    await getCollectionIfOwner(collection_id, user_id);
    
    const item = await query('SELECT * FROM items WHERE id = $1', [item_id]);
    if (item.rows.length === 0) {
      throw new NotFoundError('Không tìm thấy sản phẩm');
    }

    const existing = await query(
      'SELECT * FROM collection_items WHERE collection_id = $1 AND item_id = $2',
      [collection_id, item_id]
    );

    if (existing.rows.length > 0) {
      await query(
        `UPDATE collection_items 
         SET note = COALESCE($1, note),
             rating = COALESCE($2, rating),
             added_at = NOW()
         WHERE collection_id = $3 AND item_id = $4`,
        [note, rating, collection_id, item_id]
      );
    } else {
      await query(
        `INSERT INTO collection_items 
         (collection_id, item_id, note, rating)
         VALUES ($1, $2, $3, $4)`,
        [collection_id, item_id, note, rating]
      );
    }

    await query(
      `INSERT INTO activities 
       (user_id, activity_type, collection_id, item_id)
       VALUES ($1, 'add_item', $2, $3)`,
      [user_id, collection_id, item_id]
    );

    await query(
      'UPDATE collections SET updated_at = NOW() WHERE id = $1',
      [collection_id]
    );

    res.status(201).json({
      success: true,
      message: 'Đã thêm sản phẩm vào bộ sưu tập'
    });
  } catch (error) {
    next(error);
  }
};

const removeItemFromCollection = async (req, res, next) => {
  try {
    const { collectionId, itemId } = req.params;
    const user_id = req.user.id;

    await getCollectionIfOwner(collectionId, user_id);
    
    const result = await query(
      'DELETE FROM collection_items WHERE collection_id = $1 AND item_id = $2',
      [collectionId, itemId]
    );

    if (result.rowCount === 0) {
      throw new NotFoundError('Không tìm thấy sản phẩm trong bộ sưu tập');
    }

    await query(
      'UPDATE collections SET updated_at = NOW() WHERE id = $1',
      [collectionId]
    );

    res.json({
      success: true,
      message: 'Đã xóa sản phẩm khỏi bộ sưu tập'
    });
  } catch (error) {
    next(error);
  }
};

// Helper functions
const getCollectionIfOwner = async (collection_id, user_id) => {
  const collection = await query(
    'SELECT * FROM collections WHERE id = $1',
    [collection_id]
  );

  if (collection.rows.length === 0) {
    throw new NotFoundError('Không tìm thấy bộ sưu tập');
  }

  if (collection.rows[0].owner_id !== user_id) {
    throw new ForbiddenError('Bạn không có quyền thực hiện hành động này');
  }

  return collection.rows[0];
};

const addCollectionTags = async (collection_id, tags) => {
  try {
    console.log('=== ADD COLLECTION TAGS ===');
    console.log('Collection ID:', collection_id);
    console.log('Tags:', tags);
    
    // Thêm timeout cho từng query tag
    const tagQueries = tags.map(tag => {
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Tag query timeout')), 5000);
      });
      
      return Promise.race([
        query(
          `INSERT INTO tags (name) 
           VALUES ($1) 
           ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
           RETURNING id`,
          [tag]
        ).then(result => result.rows[0].id),
        timeoutPromise
      ]);
    });

    console.log('Bắt đầu xử lý tag queries...');
    const tagIds = await Promise.all(tagQueries);
    console.log('Tag IDs:', tagIds);

    // Thêm timeout cho link queries
    const linkQueries = tagIds.map(tagId => {
      const timeoutPromise = new Promise((_, reject) => {
        setTimeout(() => reject(new Error('Link query timeout')), 5000);
      });
      
      return Promise.race([
        query(
          `INSERT INTO collection_tags (collection_id, tag_id)
           VALUES ($1, $2)
           ON CONFLICT (collection_id, tag_id) DO NOTHING`,
          [collection_id, tagId]
        ),
        timeoutPromise
      ]);
    });

    console.log('Bắt đầu xử lý link queries...');
    await Promise.all(linkQueries);
    console.log('Hoàn thành addCollectionTags');
  } catch (error) {
    console.error('Lỗi trong addCollectionTags:', error);
    throw error;
  }
};

module.exports = {
  createCollection,
  getCollections,
  getCollectionById,
  updateCollection,
  deleteCollection,
  addItemToCollection,
  removeItemFromCollection
};
