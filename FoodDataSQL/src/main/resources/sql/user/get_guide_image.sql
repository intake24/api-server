SELECT gi.id, gi.description, pi1.path AS base_image_path, pi2.path AS selection_image_path
FROM guide_images AS gi
  JOIN image_maps AS im ON im.id = gi.image_map_id
  JOIN processed_images AS pi1 ON im.base_image_id = pi1.id
  JOIN processed_images AS pi2 ON gi.selection_image_id = pi2.id
WHERE gi.id={id}
