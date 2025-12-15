// config/db-connection.js
const { Pool } = require('pg');
require('dotenv').config();

console.log('Đang kết nối đến cơ sở dữ liệu...');
console.log('Database URL:', process.env.DATABASE_URL ? 'Đã cấu hình' : 'Chưa cấu hình');

// Tăng thời gian chờ kết nối lên 10 giây
const poolConfig = {
  connectionString: process.env.DATABASE_URL,
  // Sử dụng SSL trong môi trường production
  ssl: process.env.NODE_ENV === 'production' ? { 
    rejectUnauthorized: false 
  } : false,
  max: 20,                    // Số kết nối tối đa
  idleTimeoutMillis: 30000,   // Thời gian chờ tối đa khi không sử dụng
  connectionTimeoutMillis: 30000, // Tăng thời gian chờ kết nối lên 30 giây
  // Thử kết nối lại nếu thất bại
  retry: {
    max: 3,                   // Số lần thử lại tối đa
    timeout: 30000            // Thời gian chờ giữa các lần thử (ms)
  }
};

console.log('Cấu hình kết nối database:', {
  ssl: process.env.NODE_ENV === 'production',
  max: poolConfig.max,
  connectionTimeout: poolConfig.connectionTimeoutMillis + 'ms'
});

const pool = new Pool(poolConfig);

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

// Hàm thực thi query với xử lý lỗi và thử lại
const query = async (text, params, maxRetries = 3) => {
  const start = Date.now();
  let lastError;
  
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      console.log(`🔄 [Lần thử ${attempt}/${maxRetries}] Thực hiện truy vấn:`, { 
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
      lastError = error;
      console.error(`❌ Lỗi khi thực thi query (lần thử ${attempt}/${maxRetries}):`, {
        error: error.message,
        query: text,
        params: params || 'Không có tham số',
        stack: error.stack
      });
      
      // Nếu không phải lần thử cuối cùng thì đợi một chút trước khi thử lại
      if (attempt < maxRetries) {
        const delay = Math.pow(2, attempt) * 100; // Exponential backoff
        console.log(`⏳ Đợi ${delay}ms trước khi thử lại...`);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
  }
  
  // Nếu đã thử hết số lần mà vẫn lỗi
  console.error(`❌ Đã thử lại ${maxRetries} lần nhưng không thành công`);
  throw lastError;
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