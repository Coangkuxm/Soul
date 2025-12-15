// config/db-connection.js
const { Pool } = require('pg');
require('dotenv').config();

console.log('Đang kết nối đến cơ sở dữ liệu...');
console.log('Database URL:', process.env.DATABASE_URL ? 'Đã cấu hình' : 'Chưa cấu hình');

const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false,
  max: 20, // Số kết nối tối đa
  idleTimeoutMillis: 30000, // Thời gian chờ tối đa
  connectionTimeoutMillis: 2000 // Thời gian chờ kết nối
});

// Sự kiện khi kết nối mới được tạo
pool.on('connect', (client) => {
  console.log('✅ Đã tạo kết nối mới đến cơ sở dữ liệu');
  
  // Thiết lập múi giờ cho client
  client.query('SET timezone = "+7"');
  
  // Thiết lập schema mặc định nếu cần
  if (process.env.DB_SCHEMA) {
    client.query(`SET search_path TO ${process.env.DB_SCHEMA}`);
  }
});

// Xử lý lỗi kết nối
pool.on('error', (err) => {
  console.error('❌ Lỗi không mong muốn trên client cơ sở dữ liệu', err);
  process.exit(-1);
});

// Hàm thực thi query với xử lý lỗi
const query = async (text, params) => {
  const start = Date.now();
  try {
    console.log('🔄 Thực hiện truy vấn:', { 
      query: text, 
      params: params || 'Không có tham số' 
    });
    
    const res = await pool.query(text, params);
    const duration = Date.now() - start;
    
    // Log query chậm (lớn hơn 1s)
    if (duration > 1000) {
      console.warn(`⚠️ Query chậm (${duration}ms):`, { 
        query: text, 
        duration, 
        rows: res.rowCount 
      });
    } else {
      console.log(`✅ Query thành công (${duration}ms)`, { 
        query: text, 
        rowCount: res.rowCount 
      });
    }
    
    return res;
  } catch (error) {
    console.error('❌ Lỗi khi thực thi query:', {
      error: error.message,
      query: text,
      params: params || 'Không có tham số',
      stack: error.stack
    });
    throw error; // Ném lỗi để xử lý ở tầng trên
  }
};

// Kiểm tra kết nối khi khởi động
const checkConnection = async () => {
  try {
    const res = await query('SELECT NOW()');
    console.log('✅ Kết nối cơ sở dữ liệu thành công. Thời gian hiện tại:', res.rows[0].now);
    return true;
  } catch (error) {
    console.error('❌ Không thể kết nối đến cơ sở dữ liệu:', error.message);
    process.exit(1);
  }
};

// Gọi hàm kiểm tra kết nối khi khởi động
checkConnection();

module.exports = {
  query,
  pool,
  checkConnection
};