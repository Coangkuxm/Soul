const { Pool } = require('pg');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

async function runMigration() {
  const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
  });

  const client = await pool.connect();

  try {
    await client.query('BEGIN');
    
    console.log('🔄 Đang chạy migration...');
    
    // Đọc và thực thi từng file migration theo thứ tự
    const migrationsDir = path.join(__dirname, '..', 'migrations');
    const migrationFiles = fs.readdirSync(migrationsDir)
      .filter(file => file.endsWith('.sql'))
      .sort();

    for (const file of migrationFiles) {
      console.log(`\n📝 Đang chạy migration: ${file}`);
      const migration = fs.readFileSync(path.join(migrationsDir, file), 'utf8');
      await client.query(migration);
      console.log(`✅ Đã chạy xong: ${file}`);
    }

    await client.query('COMMIT');
    console.log('\n✨ Tất cả migrations đã được áp dụng thành công!');
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('❌ Lỗi khi chạy migration:', error);
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

runMigration().catch(err => {
  console.error('❌ Có lỗi xảy ra:', err);
  process.exit(1);
});
