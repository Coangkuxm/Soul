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

// Sự kiện khi kết nối mới được tạo
pool.on('connect', (client) => {
  console.log('Đã tạo kết nối mới đến cơ sở dữ liệu');
  
  // Thiết lập múi giờ cho client
  client.query('SET timezone = "+7"');
  
  // Thiết lập schema mặc định nếu cần
  if (process.env.DB_SCHEMA) {
    client.query(`SET search_path TO ${process.env.DB_SCHEMA}`);
  }
});

// Xử lý lỗi kết nối
pool.on('error', (err) => {
  console.error('Lỗi không mong muốn trên client cơ sở dữ liệu', err);
  process.exit(-1); // Thoát ứng dụng nếu không thể kết nối đến database
});

// Hàm thực thi query với xử lý lỗi
const query = async (text, params) => {
  const start = Date.now();
  try {
    const res = await pool.query(text, params);
    const duration = Date.now() - start;
    
    // Log query chậm (lớn hơn 1s)
    if (duration > 1000) {
      console.log('Query chậm được thực thi', { text, duration, rows: res.rowCount });
    }
    
    return res;
  } catch (error) {
    console.error('Lỗi khi thực thi query:', {
      error: error.message,
      query: text,
      params: params ? JSON.stringify(params) : 'none',
      stack: error.stack
    });
    throw error; // Ném lỗi để xử lý ở middleware
  }
};

// Hàm lấy client từ pool để thực hiện transaction
const getClient = async () => {
  const client = await pool.connect();
  
  // Override hàm query để log thời gian thực thi
  const query = client.query.bind(client);
  const release = client.release.bind(client);
  
  // Giới hạn thời gian thực thi query
  client.query = async (text, params) => {
    const start = Date.now();
    try {
      const res = await query(text, params);
      const duration = Date.now() - start;
      
      if (duration > 1000) {
        console.log('Query chậm trong transaction', { text, duration });
      }
      
      return res;
    } catch (error) {
      console.error('Lỗi trong transaction:', { error: error.message, query: text });
      throw error;
    }
  };
  
  // Đảm bảo client luôn được giải phóng
  client.release = () => {
    // Reset các phương thức về mặc định
    client.query = query;
    client.release = release;
    return release();
  };
  
  return client;
};

// Kiểm tra kết nối khi khởi động
const testConnection = async () => {
  try {
    const res = await query('SELECT NOW()');
    console.log('✅ Kết nối cơ sở dữ liệu thành công:', res.rows[0].now);
    return true;
  } catch (error) {
    console.error('❌ Kết nối cơ sở dữ liệu thất bại:', error.message);
    process.exit(1); // Thoát ứng dụng nếu không kết nối được database
  }
};

// Tự động kiểm tra kết nối khi load module
testConnection();

module.exports = {
  query,
  getClient,
  pool, // Export pool để sử dụng trực tiếp nếu cần
};
