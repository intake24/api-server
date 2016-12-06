SELECT imo.outline_coordinates AS outline, gio.object_id AS object_id, gio.description AS object_description, 
       gio.weight AS object_weight, pi.path AS overlay_image_path
FROM guide_images AS gi
  JOIN image_maps AS im ON im.id = gi.image_map_id
  JOIN image_map_objects AS imo ON imo.image_map_id = im.id
  JOIN guide_image_objects AS gio ON gio.guide_image_id = gi.id AND gio.object_id = imo.id
  JOIN processed_images AS pi ON imo.overlay_image_id = pi.id
WHERE gi.id={id}
ORDER BY imo.navigation_index ASC
