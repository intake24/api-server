SELECT id, path, thumbnail_path, uploader, uploaded_at, array_remove(array_agg(keyword), NULL) AS keywords
FROM source_images LEFT JOIN source_image_keywords ON source_images.id = source_image_keywords.source_image_id
GROUP BY id
ORDER BY id
OFFSET {offset}
LIMIT {limit}
