-- Fix collection_items table schema
-- Remove incorrect UNIQUE constraints and add correct composite constraint

-- First, drop the incorrect individual UNIQUE constraints if they exist
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'collection_items_collection_id_key') THEN
        ALTER TABLE collection_items DROP CONSTRAINT collection_items_collection_id_key;
    END IF;
    
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'collection_items_item_id_key') THEN
        ALTER TABLE collection_items DROP CONSTRAINT collection_items_item_id_key;
    END IF;
END $$;

-- Ensure the correct composite UNIQUE constraint exists
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'collection_items_collection_id_item_id_key') THEN
        ALTER TABLE collection_items ADD CONSTRAINT collection_items_collection_id_item_id_key UNIQUE(collection_id, item_id);
    END IF;
END $$;
