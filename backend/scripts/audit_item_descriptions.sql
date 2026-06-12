SELECT
  i.id AS item_id,
  i.title,
  i.external_id,
  i.created_by,
  COUNT(ci.id) AS post_count,
  STRING_AGG(DISTINCT COALESCE(NULLIF(BTRIM(ci.note), ''), '[no-note]'), ' | ') AS post_notes,
  i.description AS item_description
FROM items i
JOIN collection_items ci ON ci.item_id = i.id
GROUP BY i.id, i.title, i.external_id, i.created_by, i.description
HAVING COUNT(ci.id) > 1
   AND i.description IS NOT NULL
   AND BTRIM(i.description) <> ''
ORDER BY post_count DESC, i.id DESC;
