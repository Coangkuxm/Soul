-- Thêm cột spotify_id nếu chưa tồn tại
ALTER TABLE collection_items 
ADD COLUMN IF NOT EXISTS spotify_id VARCHAR(100);

-- Thêm cột spotify_type nếu chưa tồn tại
ALTER TABLE collection_items 
ADD COLUMN IF NOT EXISTS spotify_type VARCHAR(20);

-- Thêm ràng buộc CHECK cho spotify_type
ALTER TABLE collection_items 
ADD CONSTRAINT check_spotify_type 
CHECK (spotify_type IS NULL OR spotify_type IN ('track', 'album', 'artist', 'playlist'));

-- Thêm cột metadata kiểu JSONB
ALTER TABLE collection_items 
ADD COLUMN IF NOT EXISTS metadata JSONB;

-- Xóa ràng buộc cũ nếu tồn tại
ALTER TABLE collection_items 
DROP CONSTRAINT IF EXISTS collection_items_collection_id_item_id_key;

-- Tạo chỉ mục duy nhất mới
CREATE UNIQUE INDEX IF NOT EXISTS collection_items_unique 
ON collection_items (collection_id, COALESCE(item_id, '00000000-0000-0000-0000-000000000000'::uuid), COALESCE(spotify_id, ''));

-- Thêm chỉ mục cho spotify_id
CREATE INDEX IF NOT EXISTS idx_collection_items_spotify_id 
ON collection_items(spotify_id) 
WHERE spotify_id IS NOT NULL;

-- Thêm ràng buộc kiểm tra
ALTER TABLE collection_items 
ADD CONSTRAINT check_item_or_spotify 
CHECK (item_id IS NOT NULL OR spotify_id IS NOT NULL);