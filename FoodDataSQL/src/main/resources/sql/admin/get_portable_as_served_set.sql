SELECT description, source_images.path AS selection_source_path, processed_images.path AS selection_image_path 
  FROM as_served_sets 
    JOIN processed_images ON processed_images.id = selection_image_id
    JOIN source_images ON source_images.id = processed_images.source_id
WHERE as_served_sets.id={id}
