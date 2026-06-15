const { Pool } = require('pg');

// Cập nhật ràng buộc comments.target_type để chấp nhận 'collection_item'
// (feed post là một collection_item). DB cũ chỉ cho 'collection'/'item' -> comment feed bị 500.
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false } // Neon yêu cầu SSL
});

async function runMigration() {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    await client.query('ALTER TABLE comments DROP CONSTRAINT IF EXISTS comments_target_type_check');
    await client.query(`
      ALTER TABLE comments
      ADD CONSTRAINT comments_target_type_check
      CHECK (target_type IN ('collection', 'item', 'collection_item'))
    `);

    await client.query('COMMIT');
    console.log("OK: comments_target_type_check -> ['collection','item','collection_item']");
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Migration failed:', error);
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

runMigration().catch(() => process.exit(1));
