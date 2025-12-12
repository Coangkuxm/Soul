const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

// Lấy thông tin kết nối từ biến môi trường
const connectionString = process.env.DATABASE_URL || 
  'postgresql://neondb_owner:npg_CtDi5MFc7XEA@ep-fancy-fire-a1ar04ra-pooler.ap-southeast-1.aws.neon.tech/neondb?sslmode=require';

const pool = new Pool({
  connectionString: connectionString,
  ssl: {
    rejectUnauthorized: false // Cần thiết cho Neon
  }
});

async function runSchema() {
  const client = await pool.connect();
  try {
    console.log('🔄 Đang kết nối đến database...');
    
    // Đọc file schema.sql
    const schemaPath = path.join(__dirname, 'schema.sql');
    console.log(`📂 Đang đọc file: ${schemaPath}`);
    
    const schema = fs.readFileSync(schemaPath, 'utf8');
    
    console.log('🚀 Đang tạo các bảng...');
    
    // Thực thi từng câu lệnh SQL
    await client.query(schema);
    
    console.log('✅ Tất cả các bảng đã được tạo thành công!');
    console.log('🎉 Cơ sở dữ liệu đã sẵn sàng để sử dụng!');
    
  } catch (err) {
    console.error('❌ Lỗi khi tạo schema:');
    console.error(err.message);
    
    // In ra thông báo lỗi chi tiết hơn nếu có
    if (err.position) {
      const position = parseInt(err.position);
      const errorLine = schema.substr(0, position).split('\n').length;
      console.error(`Lỗi tại dòng ${errorLine}:`, err.message);
    }
    
    process.exit(1);
  } finally {
    client.release();
    await pool.end();
    process.exit(0);
  }
}

// Xử lý lỗi không mong muốn
process.on('unhandledRejection', (err) => {
  console.error('❌ Có lỗi không mong muốn:');
  console.error(err);
  process.exit(1);
});

// Chạy hàm chính
runSchema();
