const { Pool } = require('pg');
require('dotenv').config();

// Tạo pool kết nối đến database
const pool = new Pool({
  connectionString: process.env.DATABASE_URL || 'postgresql://neondb_owner:npg_CtDi5MFc7XEA@ep-fancy-fire-a1ar04ra-pooler.ap-southeast-1.aws.neon.tech/neondb?sslmode=require',
  ssl: {
    rejectUnauthorized: false // Cần thiết cho Neon
  },
  // Tối ưu kết nối
  connectionTimeoutMillis: 5000,
  idleTimeoutMillis: 30000,
  max: 20
});

// Kiểm tra kết nối khi khởi động
(async () => {
  try {
    const client = await pool.connect();
    console.log('✅ Đã kết nối thành công tới database');
    client.release();
  } catch (error) {
    console.error('❌ Không thể kết nối đến database:', error.message);
    process.exit(1);
  }
})();

// Xử lý lỗi kết nối
pool.on('error', (err) => {
  console.error('❌ Lỗi kết nối database không mong muốn:', err.message);
  process.exit(-1);
});

module.exports = {
  query: (text, params) => pool.query(text, params),
  getClient: () => pool.connect(),
  pool
};
