// backend/models/user.model.js
const { query } = require('../config/db-connection');

const userModel = {
  // Đếm tổng số người dùng (có thể lọc theo tìm kiếm)
  async countUsers(search = '') {
    let queryText = `SELECT COUNT(*)
                     FROM users
                     WHERE COALESCE(account_status, 'active') = 'active'`;
    const queryParams = [];
    
    if (search) {
      queryText += ' AND (username ILIKE $1 OR email ILIKE $1 OR display_name ILIKE $1)';
      queryParams.push(`%${search}%`);
    }
    
    const result = await query(queryText, queryParams);
    return parseInt(result.rows[0].count, 10);
  },
  // Tìm user bằng ID (kèm số liệu follower/following/collection)
  async findById(id) {
    const result = await query(
      `SELECT
        id,
        username,
        email,
        display_name as "displayName",
        avatar_url as "avatarUrl",
        bio,
        role,
        account_status as "accountStatus",
        (SELECT COUNT(*)::int FROM user_follows WHERE following_id = users.id) as "followerCount",
        (SELECT COUNT(*)::int FROM user_follows WHERE follower_id = users.id) as "followingCount",
        (SELECT COUNT(*)::int FROM collections WHERE owner_id = users.id) as "collectionCount",
        created_at as "createdAt",
        updated_at as "updatedAt"
      FROM users
      WHERE id = $1
        AND COALESCE(account_status, 'active') = 'active'`,
      [id]
    );
    return result.rows[0];
  },

  // Tìm user bằng email
  async findByEmail(email) {
    const result = await query(
      'SELECT * FROM users WHERE email = $1',
      [email]
    );
    return result.rows[0];
  },

  async findByUsername(username) {
    const result = await query(
      'SELECT * FROM users WHERE username = $1',
      [username]
    );
    return result.rows[0];
  },

  // Tạo user mới
  async create({ username, email, password, displayName, avatarUrl, bio }) {
    const result = await query(
      `INSERT INTO users (
        username, email, password, display_name, avatar_url, bio
      ) VALUES ($1, $2, $3, $4, $5, $6) 
      RETURNING id, username, email, display_name as "displayName", 
                avatar_url as "avatarUrl", bio, role,
                account_status as "accountStatus", created_at as "createdAt"`,
      [username, email, password, displayName, avatarUrl, bio]
    );
    return result.rows[0];
  },

  // Cập nhật thông tin user
  async update(id, { username, email, displayName, avatarUrl, bio }) {
    const result = await query(
      `UPDATE users 
       SET username = COALESCE($1, username),
           email = COALESCE($2, email),
           display_name = COALESCE($3, display_name),
           avatar_url = CASE 
             WHEN $4 = '' THEN NULL
             ELSE COALESCE($4, avatar_url)
           END,
           bio = COALESCE($5, bio),
           updated_at = NOW()
       WHERE id = $6
       RETURNING id, username, email, display_name as "displayName", 
                 avatar_url as "avatarUrl", bio, role,
                 account_status as "accountStatus", updated_at as "updatedAt"`,
      [username, email, displayName, avatarUrl, bio, id]
    );
    return result.rows[0];
  },

  // Đổi mật khẩu
  async changePassword(id, newPassword) {
    await query(
      'UPDATE users SET password = $1, updated_at = NOW() WHERE id = $2',
      [newPassword, id]
    );
    return true;
  },

  // Lấy danh sách người dùng (phân trang)
  async getAll({ page = 1, limit = 10, search = '', currentUserId = null }) {
    const offset = (page - 1) * limit;
    const queryParams = [];
    const currentUserParam = currentUserId ? queryParams.push(currentUserId) : null;
    const searchParam = search ? queryParams.push(`%${search}%`) : null;

    let queryText = `
      SELECT 
        id, 
        username, 
        email, 
        display_name as "displayName", 
        avatar_url as "avatarUrl", 
        bio, 
        role,
        account_status as "accountStatus",
        ${
          currentUserParam
            ? `EXISTS (
                SELECT 1 FROM user_follows uf
                WHERE uf.follower_id = $${currentUserParam}
                  AND uf.following_id = users.id
              )`
            : 'false'
        } as "isFollowing",
        (
          SELECT COUNT(*)::int
          FROM user_follows uf_count
          WHERE uf_count.following_id = users.id
        ) as "followerCount",
        created_at as "createdAt",
        updated_at as "updatedAt"
      FROM users
      WHERE COALESCE(account_status, 'active') = 'active'
    `;

    if (currentUserParam) {
      queryText += ` AND id <> $${currentUserParam}`;
    }

    if (search) {
      queryText += ` AND (username ILIKE $${searchParam} OR email ILIKE $${searchParam} OR display_name ILIKE $${searchParam})`;
      queryText += ` ORDER BY "isFollowing" DESC, username ASC`;
    } else {
      queryText += ` ORDER BY "isFollowing" DESC, "followerCount" DESC, created_at DESC`;
    }

    queryText += ` LIMIT $${queryParams.length + 1} OFFSET $${queryParams.length + 2}`;
    
    queryParams.push(limit, offset);
    
    const result = await query(queryText, queryParams);
    const total = await this.countUsers(search);
    
    return {
      users: result.rows,
      pagination: {
        total,
        page: parseInt(page, 10),
        limit: parseInt(limit, 10),
        totalPages: Math.ceil(total / limit)
      }
    };
  },

  // Xóa người dùng
  async delete(id) {
    await query('DELETE FROM users WHERE id = $1', [id]);
    return true;
  },

  // Theo dõi người dùng
  async followUser(followerId, followingId) {
    await query(
      'INSERT INTO user_follows (follower_id, following_id) VALUES ($1, $2) ON CONFLICT DO NOTHING',
      [followerId, followingId]
    );
    return true;
  },

  // Bỏ theo dõi
  async unfollowUser(followerId, followingId) {
    await query(
      'DELETE FROM user_follows WHERE follower_id = $1 AND following_id = $2',
      [followerId, followingId]
    );
    return true;
  },

  // Kiểm tra đang theo dõi
  async isFollowing(followerId, followingId) {
    const result = await query(
      'SELECT 1 FROM user_follows WHERE follower_id = $1 AND following_id = $2',
      [followerId, followingId]
    );
    return result.rows.length > 0;
  },

  // Lấy danh sách người theo dõi
  async getFollowers(userId) {
    const result = await query(
      `SELECT 
        u.id, u.username, u.display_name as "displayName", 
        u.avatar_url as "avatarUrl"
       FROM user_follows uf
       JOIN users u ON uf.follower_id = u.id
       WHERE uf.following_id = $1
         AND COALESCE(u.account_status, 'active') = 'active'`,
      [userId]
    );
    return result.rows;
  },

  // Lấy danh sách đang theo dõi
  async getFollowing(userId) {
    const result = await query(
      `SELECT 
        u.id, u.username, u.display_name as "displayName", 
        u.avatar_url as "avatarUrl"
       FROM user_follows uf
       JOIN users u ON uf.following_id = u.id
       WHERE uf.follower_id = $1
         AND COALESCE(u.account_status, 'active') = 'active'`,
      [userId]
    );
    return result.rows;
  }
};

module.exports = userModel;
