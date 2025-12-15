// Test endpoint để kiểm tra database connection
app.get('/test-db', async (req, res) => {
  try {
    console.log('=== TEST DB CONNECTION ===');
    console.log('Bắt đầu test query...');
    
    const startTime = Date.now();
    const result = await query('SELECT NOW() as current_time');
    const duration = Date.now() - startTime;
    
    console.log(`Test query thành công trong ${duration}ms`);
    
    res.json({
      success: true,
      message: 'Database connection OK',
      time: result.rows[0].current_time,
      duration: `${duration}ms`
    });
  } catch (error) {
    console.error('Test query lỗi:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});
