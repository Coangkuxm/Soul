// backend/models/user.model.js
const { query } = require('../config/db');

const userModel = {
  // Tìm user bằng ID
  async findById(id) {
    const result = await query(
      `SELECT 
        id, 
        username, 
        email, 
        display_name as "displayName", 
        avatar_url as "avatarUrl", 
        bio, 
        created_at as "createdAt",
        updated_at as "updatedAt"
      FROM users 
      WHERE id = $1`,
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

  // Tạo user mới
  async create({ username, email, password, displayName, avatarUrl, bio }) {
    const result = await query(
      `INSERT INTO users (
        username, email, password, display_name, avatar_url, bio
      ) VALUES ($1, $2, $3, $4, $5, $6) 
      RETURNING id, username, email, display_name as "displayName", 
                avatar_url as "avatarUrl", bio, created_at as "createdAt"`,
      [username, email, password, displayName, avatarUrl, bio]
    );
    return result.rows[0];
  },

  // Cập nhật thông tin user
  async update(id, { displayName, avatarUrl, bio }) {
    const result = await query(
      `UPDATE users 
       SET display_name = COALESCE($1, display_name),
           avatar_url = COALESCE($2, avatar_url),
           bio = COALESCE($3, bio),
           updated_at = NOW()
       WHERE id = $4
       RETURNING id, username, email, display_name as "displayName", 
                 avatar_url as "avatarUrl", bio, updated_at as "updatedAt"`,
      [displayName, avatarUrl, bio, id]
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
  async getAll({ page = 1, limit = 10, search = '' }) {
    const offset = (page - 1) * limit;
    const result = await query(
      `SELECT 
        id, username, email, display_name as "displayName", 
        avatar_url as "avatarUrl", created_at as "createdAt"
       FROM users
       WHERE username ILIKE $1 OR email ILIKE $1
       ORDER BY created_at DESC
       LIMIT $2 OFFSET $3`,
      [`%${search}%`, limit, offset]
    );
    return result.rows;
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
       WHERE uf.following_id = $1`,
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
       WHERE uf.follower_id = $1`,
      [userId]
    );
    return result.rows;
  }
};

module.exports = userModel;