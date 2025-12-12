const db = require('./db');

async function getData() {
  try {
    const result = await db.query('SELECT NOW()');
    console.log('Kết nối thành công! Thời gian hiện tại:', result.rows[0].now);
  } catch (err) {
    console.error('Lỗi kết nối:', err);
  }
}

// Thêm dòng này để đảm bảo hàm bất đồng bộ được chạy
getData().catch(console.error);