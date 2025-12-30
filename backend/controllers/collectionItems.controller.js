const { query } = require('../config/db-connection');
const { DatabaseError, NotFoundError, ForbiddenError } = require('../utils/errors');
const { getSpotifyTrack, getSpotifyAlbum, parseSpotifyUrl } = require('../utils/spotifyCollectionHelper');

// Helper function to check collection ownership
async function checkCollectionOwnership(collectionId, userId) {
  const collectionCheck = await query(
    'SELECT created_by, is_public FROM collections WHERE id = $1',
    [collectionId]
  );

  if (collectionCheck.rows.length === 0) {
    throw new NotFoundError('Không tìm thấy collection');
  }

  const collection = collectionCheck.rows[0];
  
  // Allow access if collection is public and user is just viewing
  if (collection.is_public && !userId) {
    return { isOwner: false, isPublic: true };
  }

  // If user is authenticated, check ownership
  if (userId && collection.created_by === userId) {
    return { isOwner: true, isPublic: collection.is_public };
  }

  // If we get here, user doesn't have access
  throw new ForbiddenError('Bạn không có quyền truy cập collection này');
}

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

  // Lấy danh sách items trong collection (cả items thường và Spotify)
  async getItemsInCollection(req, res, next) {
    try {
      const { collection_id } = req.params;
      const { page = 1, limit = 10, type } = req.query;
      const offset = (page - 1) * limit;
      const userId = req.user?.id;

      // Kiểm tra quyền truy cập collection
      const { isOwner, isPublic } = await checkCollectionOwnership(collection_id, userId);
      
      // Nếu không phải chủ sở hữu và collection không public
      if (!isOwner && !isPublic) {
        throw new ForbiddenError('Bạn không có quyền xem collection này');
      }

      // Xây dựng điều kiện WHERE dựa trên type
      let whereClause = 'WHERE ci.collection_id = $1';
      const queryParams = [collection_id];
      let paramIndex = 2;

      if (type === 'spotify') {
        whereClause += ` AND ci.spotify_id IS NOT NULL`;
        if (req.query.spotify_type) {
          whereClause += ` AND ci.spotify_type = $${paramIndex++}`;
          queryParams.push(req.query.spotify_type);
        }
      } else if (type === 'regular') {
        whereClause += ' AND ci.spotify_id IS NULL';
      }

      // Lấy tổng số items
      const countQuery = {
        text: `SELECT COUNT(*) FROM collection_items ci ${whereClause}`,
        values: queryParams
      };
      
      const countResult = await query(countQuery);
      const total = parseInt(countResult.rows[0].count);

      // Lấy danh sách items
      const itemsQuery = {
        text: `
          SELECT 
            ci.id as collection_item_id,
            ci.spotify_id,
            ci.spotify_type,
            ci.metadata as spotify_metadata,
            ci.created_at as added_at,
            i.*
          FROM collection_items ci
          LEFT JOIN items i ON i.id = ci.item_id
          ${whereClause}
          ORDER BY ci.created_at DESC
          LIMIT $${paramIndex} OFFSET $${paramIndex + 1}
        `,
        values: [...queryParams, limit, offset]
      };

      const itemsResult = await query(itemsQuery);
      
      // Chia thành items thường và Spotify items
      const regularItems = [];
      const spotifyItems = [];
      
      for (const item of itemsResult.rows) {
        if (item.spotify_id) {
          spotifyItems.push({
            id: item.collection_item_id,
            spotify_id: item.spotify_id,
            spotify_type: item.spotify_type,
            metadata: item.spotify_metadata,
            added_at: item.added_at,
            type: 'spotify'
          });
        } else if (item.id) {
          regularItems.push({
            ...item,
            type: 'regular'
          });
        }
      }

      res.json({
        success: true,
        data: [...regularItems, ...spotifyItems],
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
  },

  // Thêm Spotify item vào collection
  async addSpotifyItemToCollection(req, res, next) {
    try {
      const { collection_id, spotify_id, spotify_type, metadata } = req.body;
      const user_id = req.user.id;

      // Kiểm tra quyền sở hữu collection
      await checkCollectionOwnership(collection_id, user_id);

      // Kiểm tra spotify_type hợp lệ
      const validTypes = ['track', 'album', 'artist', 'playlist'];
      if (!validTypes.includes(spotify_type)) {
        throw new Error('Loại Spotify không hợp lệ');
      }

      // Kiểm tra item đã có trong collection chưa
      const exists = await query(
        'SELECT 1 FROM collection_items WHERE collection_id = $1 AND spotify_id = $2',
        [collection_id, spotify_id]
      );

      if (exists.rows.length > 0) {
        return res.status(200).json({
          success: true,
          message: 'Spotify item đã có trong collection',
          data: { collection_id, spotify_id, spotify_type }
        });
      }

      // Thêm Spotify item vào collection
      const result = await query(
        `INSERT INTO collection_items 
         (collection_id, spotify_id, spotify_type, metadata, created_by) 
         VALUES ($1, $2, $3, $4, $5) 
         RETURNING id, collection_id, spotify_id, spotify_type, created_at`,
        [collection_id, spotify_id, spotify_type, metadata || null, user_id]
      );

      res.status(201).json({
        success: true,
        message: 'Đã thêm Spotify item vào collection',
        data: result.rows[0]
      });
    } catch (error) {
      next(new DatabaseError('Lỗi khi thêm Spotify item vào collection: ' + error.message));
    }
  },

  // Xóa Spotify item khỏi collection
  async removeSpotifyItemFromCollection(req, res, next) {
    try {
      const { collection_id, spotify_id } = req.params;
      const user_id = req.user.id;

      // Kiểm tra quyền sở hữu collection
      await checkCollectionOwnership(collection_id, user_id);

      // Xóa Spotify item khỏi collection
      const result = await query(
        'DELETE FROM collection_items WHERE collection_id = $1 AND spotify_id = $2 RETURNING *',
        [collection_id, spotify_id]
      );

      if (result.rows.length === 0) {
        throw new NotFoundError('Không tìm thấy Spotify item trong collection');
      }

      res.json({
        success: true,
        message: 'Đã xóa Spotify item khỏi collection',
        data: result.rows[0]
      });
    } catch (error) {
      next(new DatabaseError('Lỗi khi xóa Spotify item khỏi collection: ' + error.message));
    }
  },
  
  // Tìm kiếm và thêm Spotify item bằng URL
  async addSpotifyItemByUrl(req, res, next) {
    try {
      const { collection_id, url } = req.body;
      const user_id = req.user.id;

      // Kiểm tra quyền sở hữu collection
      await checkCollectionOwnership(collection_id, user_id);

      // Phân tích URL để lấy Spotify ID và loại
      const spotifyData = parseSpotifyUrl(url);
      if (!spotifyData) {
        throw new Error('URL Spotify không hợp lệ');
      }

      // Kiểm tra item đã có trong collection chưa
      const exists = await query(
        'SELECT 1 FROM collection_items WHERE collection_id = $1 AND spotify_id = $2',
        [collection_id, spotifyData.id]
      );

      if (exists.rows.length > 0) {
        return res.status(200).json({
          success: true,
          message: 'Spotify item đã có trong collection',
          data: { collection_id, spotify_id: spotifyData.id, spotify_type: spotifyData.type }
        });
      }

      // Lấy thông tin chi tiết từ Spotify API
      let spotifyItem;
      let metadata = {};

      if (spotifyData.type === 'track') {
        spotifyItem = await getSpotifyTrack(spotifyData.id);
        metadata = {
          name: spotifyItem.name,
          artists: spotifyItem.artists,
          album: spotifyItem.album,
          duration_ms: spotifyItem.duration_ms,
          preview_url: spotifyItem.preview_url,
          external_url: spotifyItem.external_url,
          popularity: spotifyItem.popularity
        };
      } else if (spotifyData.type === 'album') {
        spotifyItem = await getSpotifyAlbum(spotifyData.id);
        metadata = {
          name: spotifyItem.name,
          artists: spotifyItem.artists,
          release_date: spotifyItem.release_date,
          total_tracks: spotifyItem.total_tracks,
          external_url: spotifyItem.external_url,
          tracks: spotifyItem.tracks
        };
      } else {
        // Chỉ hỗ trợ track và album cho phiên bản hiện tại
        throw new Error('Chỉ hỗ trợ thêm bài hát (track) và album từ Spotify');
      }

      // Thêm Spotify item vào collection
      const result = await query(
        `INSERT INTO collection_items 
         (collection_id, spotify_id, spotify_type, metadata, created_by) 
         VALUES ($1, $2, $3, $4, $5) 
         RETURNING id, collection_id, spotify_id, spotify_type, created_at`,
        [collection_id, spotifyData.id, spotifyData.type, metadata, user_id]
      );

      res.status(201).json({
        success: true,
        message: `Đã thêm ${spotifyData.type} từ Spotify vào collection`,
        data: {
          ...result.rows[0],
          spotify_data: spotifyItem
        }
      });
    } catch (error) {
      next(new DatabaseError('Lỗi khi thêm Spotify item từ URL: ' + error.message));
    }
  }
};

module.exports = collectionItemsController;
