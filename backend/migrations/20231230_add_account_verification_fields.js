const { Pool } = require('pg');

// Use environment variables for database connection
const pool = new Pool({
  connectionString: process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/soul',
  ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : false
});

async function runMigration() {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // Add reset_password_token column if not exists
    await client.query(`
      ALTER TABLE users 
      ADD COLUMN IF NOT EXISTS reset_password_token VARCHAR(255),
      ADD COLUMN IF NOT EXISTS reset_password_expires TIMESTAMP WITH TIME ZONE,
      ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE,
      ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255),
      ADD COLUMN IF NOT EXISTS email_verification_expires TIMESTAMP WITH TIME ZONE;
    `);

    await client.query('COMMIT');
    console.log('Migration completed successfully');
  } catch (error) {
    await client.query('ROLLBACK');
    console.error('Migration failed:', error);
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

runMigration().catch(console.error);
