const fs = require('fs');
const path = require('path');
const { pool, getClient } = require('./db-connection');

// Dữ liệu mẫu
// ... (giữ nguyên các import và phần đầu file)

const sampleData = {
  users: [
    {
      username: 'user1',
      email: 'user1@example.com',
      password: '123456',  // Đổi từ password_hash sang password thường
      display_name: 'Nguyễn Văn A',
      avatar_url: 'https://i.pravatar.cc/150?img=1',
      bio: 'Người yêu sách và phim ảnh'
    },
    {
      username: 'user2',
      email: 'user2@example.com',
      password: '123456',  // Đổi từ password_hash sang password thường
      display_name: 'Trần Thị B',
      avatar_url: 'https://i.pravatar.cc/150?img=2',
      bio: 'Đam mê âm nhạc và nghệ thuật'
    },
    {
      username: 'user3',
      email: 'user3@example.com',
      password: '123456',  // Đổi từ password_hash sang password thường
      display_name: 'Lê Văn C',
      avatar_url: 'https://i.pravatar.cc/150?img=3',
      bio: 'Game thủ chuyên nghiệp'
    }
  ],
  // ... (phần còn lại giữ nguyên)
};

// ... (phần còn lại của file)

// Trong hàm seedDatabase, sửa câu lệnh INSERT cho users:
const res = await client.query(
  'INSERT INTO users (username, email, password, display_name, avatar_url, bio) VALUES ($1, $2, $3, $4, $5, $6) RETURNING id',
  [user.username, user.email, user.password, user.display_name, user.avatar_url, user.bio]
);

// Hàm chính để chèn dữ liệu
async function seedDatabase() {
  const client = await pool.connect();
  
  try {
    console.log('🔄 Bắt đầu chèn dữ liệu mẫu...');
    await client.query('BEGIN');

    // 1. Chèn users
    console.log('👥 Đang thêm người dùng...');
    const insertedUsers = [];
    for (const user of sampleData.users) {
      const res = await client.query(
        'INSERT INTO users (username, email, password_hash, display_name, avatar_url, bio) VALUES ($1, $2, $3, $4, $5, $6) RETURNING id',
        [user.username, user.email, user.password_hash, user.display_name, user.avatar_url, user.bio]
      );
      insertedUsers.push({ ...user, id: res.rows[0].id });
    }
    console.log(`✅ Đã thêm ${insertedUsers.length} người dùng`);

    // 2. Chèn items
    console.log('📚 Đang thêm items...');
    const insertedItems = [];
    for (const item of sampleData.items) {
      const createdBy = insertedUsers[0].id; // Gán tất cả items cho user đầu tiên
      const res = await client.query(
        'INSERT INTO items (type, title, description, cover_image_url, external_id, metadata, created_by) VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING id',
        [item.type, item.title, item.description, item.cover_image_url, item.external_id || null, item.metadata, createdBy]
      );
      insertedItems.push({ ...item, id: res.rows[0].id });
    }
    console.log(`✅ Đã thêm ${insertedItems.length} items`);

    // 3. Chèn collections và collection_items
    console.log('📂 Đang thêm bộ sưu tập...');
    const insertedCollections = [];
    for (let i = 0; i < sampleData.collections.length; i++) {
      const collection = sampleData.collections[i];
      const ownerId = insertedUsers[i % insertedUsers.length].id; // Phân phối collections cho các user
      
      const res = await client.query(
        'INSERT INTO collections (name, description, cover_image_url, is_private, owner_id) VALUES ($1, $2, $3, $4, $5) RETURNING id',
        [collection.name, collection.description, collection.cover_image_url, collection.is_private, ownerId]
      );
      
      const collectionId = res.rows[0].id;
      insertedCollections.push({ ...collection, id: collectionId });
      
      // Thêm items vào collection
      for (const item of collection.items) {
        await client.query(
          'INSERT INTO collection_items (collection_id, item_id, note, rating) VALUES ($1, $2, $3, $4)',
          [collectionId, insertedItems[item.item_id - 1].id, item.note, item.rating]
        );
        
        // Thêm tags nếu có
        if (item.tags && item.tags.length > 0) {
          for (const tagName of item.tags) {
            // Kiểm tra tag đã tồn tại chưa
            let tagRes = await client.query('SELECT id FROM tags WHERE name = $1', [tagName]);
            let tagId;
            
            if (tagRes.rows.length === 0) {
              // Nếu tag chưa tồn tại, tạo mới
              tagRes = await client.query('INSERT INTO tags (name) VALUES ($1) RETURNING id', [tagName]);
              tagId = tagRes.rows[0].id;
            } else {
              tagId = tagRes.rows[0].id;
            }
            
            // Thêm quan hệ giữa collection và tag
            await client.query(
              'INSERT INTO collection_tags (collection_id, tag_id) VALUES ($1, $2) ON CONFLICT DO NOTHING',
              [collectionId, tagId]
            );
          }
        }
      }
    }
    console.log(`✅ Đã thêm ${insertedCollections.length} bộ sưu tập`);

    // 4. Chèn comments
    console.log('💬 Đang thêm bình luận...');
    for (const comment of sampleData.comments) {
      await client.query(
        'INSERT INTO comments (user_id, content, target_type, target_id, parent_id) VALUES ($1, $2, $3, $4, $5)',
        [
          insertedUsers[comment.user_id - 1].id,
          comment.content,
          comment.target_type,
          comment.target_id,
          comment.parent_id || null
        ]
      );
    }
    console.log(`✅ Đã thêm ${sampleData.comments.length} bình luận`);

    // 5. Chèn follows
    console.log('👥 Đang thêm quan hệ theo dõi...');
    for (const follow of sampleData.follows) {
      await client.query(
        'INSERT INTO user_follows (follower_id, following_id) VALUES ($1, $2) ON CONFLICT DO NOTHING',
        [insertedUsers[follow.follower_id - 1].id, insertedUsers[follow.following_id - 1].id]
      );
    }
    console.log(`✅ Đã thêm ${sampleData.follows.length} quan hệ theo dõi`);

    // 6. Chèn likes
    console.log('❤️  Đang thêm lượt thích...');
    for (const like of sampleData.likes) {
      await client.query(
        'INSERT INTO likes (user_id, target_id, target_type) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING',
        [insertedUsers[like.user_id - 1].id, like.target_id, like.target_type]
      );
      
      // Cập nhật like_count cho collection hoặc comment tương ứng
      if (like.target_type === 'collection') {
        await client.query(
          'UPDATE collections SET like_count = like_count + 1 WHERE id = $1',
          [like.target_id]
        );
      } else if (like.target_type === 'comment') {
        await client.query(
          'UPDATE comments SET like_count = like_count + 1 WHERE id = $1',
          [like.target_id]
        );
      }
    }
    console.log(`✅ Đã thêm ${sampleData.likes.length} lượt thích`);

    await client.query('COMMIT');
    console.log('🎉 Đã thêm dữ liệu mẫu thành công!');
    
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('❌ Lỗi khi thêm dữ liệu mẫu:', err);
    throw err;
  } finally {
    client.release();
    await pool.end();
  }
}

// Chạy hàm chính
seedDatabase().catch(err => {
  console.error('❌ Có lỗi xảy ra:', err);
  process.exit(1);
});
