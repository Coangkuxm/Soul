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
  try {
    const { page = 1, limit = 10, user_id, is_private } = req.query;
    const offset = (page - 1) * limit;
    
    let queryStr = `
      SELECT c.*, u.username, u.avatar_url as owner_avatar,
             (SELECT COUNT(*) FROM collection_items WHERE collection_id = c.id) as item_count
      FROM collections c
      JOIN users u ON c.owner_id = u.id
      WHERE 1=1
    `;
    
    const queryParams = [];
    
    if (user_id) {
      queryParams.push(user_id);
      queryStr += ` AND c.owner_id = $${queryParams.length}`;
    }
    
    if (is_private !== undefined) {
      queryParams.push(is_private === 'true');
      queryStr += ` AND c.is_private = $${queryParams.length}`;
    }
    
    if (req.user) {
      queryStr += ` AND (c.is_private = false OR c.owner_id = ${req.user.id})`;
    } else {
      queryStr += ` AND c.is_private = false`;
    }
    
    queryStr += ` ORDER BY c.created_at DESC
                 LIMIT $${queryParams.length + 1} 
                 OFFSET $${queryParams.length + 2}`;
    
    queryParams.push(limit, offset);
    
    const result = await query(queryStr, queryParams);
    
    res.json({
      success: true,
      data: result.rows,
      pagination: {
        page: parseInt(page),
        limit: parseInt(limit),
        total: parseInt((await query('SELECT COUNT(*) FROM collections WHERE is_private = false'))[0].count)
      }
    });
  } catch (error) {
    next(error);
  }
};

const getCollectionById = async (req, res, next) => {
  try {
    const { id } = req.params;
    
    const result = await query(
      `SELECT c.*, u.username, u.avatar_url as owner_avatar,
              (SELECT COUNT(*) FROM collection_items WHERE collection_id = c.id) as item_count,
              EXISTS (
                SELECT 1 FROM likes 
                WHERE target_id = c.id 
                AND target_type = 'collection' 
                AND user_id = $2
              ) as is_liked
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
  const tagQueries = tags.map(tag => 
    query(
      `INSERT INTO tags (name) 
       VALUES ($1) 
       ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
       RETURNING id`,
      [tag]
    ).then(result => result.rows[0].id)
  );

  const tagIds = await Promise.all(tagQueries);

  const linkQueries = tagIds.map(tagId => 
    query(
      `INSERT INTO collection_tags (collection_id, tag_id)
       VALUES ($1, $2)
       ON CONFLICT (collection_id, tag_id) DO NOTHING`,
      [collection_id, tagId]
    )
  );

  await Promise.all(linkQueries);
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
