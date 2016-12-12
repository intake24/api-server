SELECT gi.description, gi.image_map_id, array_agg(gio.image_map_object_id) AS object_id, array_agg(gio.weight) AS object_weight
  FROM guide_images AS gi JOIN guide_image_objects AS gio ON gi.id = gio.guide_image_id
WHERE gi.id={id}
GROUP BY(gi.description, gi.image_map_id)