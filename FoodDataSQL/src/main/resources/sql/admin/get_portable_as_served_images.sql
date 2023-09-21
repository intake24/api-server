SELECT weight,
       src.path           as source_path,
       src.thumbnail_path as source_thumbnail_path,
       kw.keywords,
       p1.path            as image_path,
       p2.path            as thumbnail_image_path
FROM as_served_images
         JOIN processed_images as p1 ON image_id = p1.id
         JOIN processed_images as p2 ON thumbnail_image_id = p2.id
         JOIN source_images as src ON src.id = p1.source_id
         LEFT JOIN (
    SELECT source_image_id, array_agg(keyword) as keywords
    FROM source_image_keywords
    GROUP BY source_image_id
) kw ON(kw.source_image_id = src.id)
WHERE as_served_set_id = {as_served_set_id}
ORDER BY weight
