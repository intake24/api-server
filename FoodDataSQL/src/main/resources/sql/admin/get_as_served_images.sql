SELECT p1.path AS image_path, p2.path AS thumbnail_image_path, weight, src.id AS source_id FROM as_served_images
  JOIN processed_images AS p1 ON image_id = p1.id
  JOIN processed_images AS p2 ON thumbnail_image_id = p2.id
  JOIN source_images AS src ON p1.source_id = src.id
WHERE as_served_set_id={as_served_set_id}
ORDER BY weight