WITH pre_filtered_keywords AS (
	SELECT source_image_id, array_remove(array_agg(source_image_keywords.keyword), NULL) AS keywords
	FROM source_image_keywords
	WHERE keyword SIMILAR TO {regex_pattern}
	GROUP BY source_image_id
), filtered_keywords AS (
  SELECT source_image_id FROM pre_filtered_keywords
  WHERE keywords @> {array_pattern}::varchar[]
)
SELECT id, path, thumbnail_path, uploader, uploaded_at, array_remove(array_agg(source_image_keywords.keyword), NULL) AS keywords
FROM source_images
RIGHT JOIN filtered_keywords
ON source_images.id = filtered_keywords.source_image_id
LEFT JOIN source_image_keywords
ON source_images.id = source_image_keywords.source_image_id
GROUP BY id
ORDER BY uploaded_at DESC
OFFSET {offset}
LIMIT {limit}