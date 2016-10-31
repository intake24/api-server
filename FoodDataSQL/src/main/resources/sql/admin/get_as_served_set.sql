SELECT description, selection_image_id, processed_images.path AS selection_image_path 
  FROM as_served_sets JOIN processed_images ON processed_images.id = selection_image_id
WHERE as_served_sets.id={id}