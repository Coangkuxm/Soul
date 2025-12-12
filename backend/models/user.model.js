// backend/models/user.model.js
const { query } = require('../config/db');

const userModel = {
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
  }
};

module.exports = userModel;