BEGIN;

-- Step 1: move safe legacy descriptions into collection_items.note
WITH single_post_items AS (
  SELECT ci.item_id
  FROM collection_items ci
  GROUP BY ci.item_id
  HAVING COUNT(ci.id) = 1
)
UPDATE collection_items ci
SET note = i.description
FROM items i
JOIN collections c ON c.id = ci.collection_id
JOIN single_post_items spi ON spi.item_id = ci.item_id
WHERE ci.item_id = i.id
  AND i.created_by = c.owner_id
  AND (ci.note IS NULL OR BTRIM(ci.note) = '')
  AND i.description IS NOT NULL
  AND BTRIM(i.description) <> '';

-- Step 2: clear only descriptions that were safely copied above
WITH single_post_items AS (
  SELECT ci.item_id
  FROM collection_items ci
  GROUP BY ci.item_id
  HAVING COUNT(ci.id) = 1
)
UPDATE items i
SET description = NULL
FROM collection_items ci
JOIN collections c ON c.id = ci.collection_id
JOIN single_post_items spi ON spi.item_id = ci.item_id
WHERE ci.item_id = i.id
  AND i.created_by = c.owner_id
  AND ci.note = i.description
  AND i.description IS NOT NULL
  AND BTRIM(i.description) <> '';

COMMIT;
