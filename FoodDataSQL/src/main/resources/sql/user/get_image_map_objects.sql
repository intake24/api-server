SELECT im.id, pi1.path as base_image_path, imo.id AS object_id, imo.description, imo.outline_coordinates AS outline, pi2.path AS overlay_image_path
FROM image_maps AS im
  JOIN image_map_objects AS imo ON imo.image_map_id = im.id
  JOIN processed_images AS pi1 ON im.base_image_id = pi1.id
  JOIN processed_images AS pi2 ON imo.overlay_image_id = pi2.id
WHERE im.id IN ({ids})
ORDER BY imo.navigation_index ASC
