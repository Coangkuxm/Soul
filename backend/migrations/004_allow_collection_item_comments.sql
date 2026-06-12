BEGIN;

ALTER TABLE comments
DROP CONSTRAINT IF EXISTS comments_target_type_check;

ALTER TABLE comments
ADD CONSTRAINT comments_target_type_check
CHECK (target_type IN ('collection', 'item', 'collection_item'));

COMMIT;
