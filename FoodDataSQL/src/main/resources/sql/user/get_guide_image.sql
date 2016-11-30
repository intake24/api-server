SELECT pi1.path as base_image_path, pi3.path as selection_image_path, gi.description as image_description, 
      array_agg(gio.object_id) as object_id, array_agg(gio.description) as object_description, 
      array_agg(gio.weight) as object_weight, array_agg(pi2.path) as overlay_image_path
FROM guide_images AS gi
  JOIN guide_image_objects AS gio ON gi.id = gio.guide_image_id 
  JOIN processed_images AS pi1 ON gi.image_id = pi1.id
  JOIN processed_images AS pi2 ON gio.overlay_image_id = pi2.id
  JOIN processed_images AS pi3 ON gi.selection_image_id = pi3.id
WHERE gi.id = {id}
GROUP BY (gi.id, pi1.path, pi3.path, gi.description)
