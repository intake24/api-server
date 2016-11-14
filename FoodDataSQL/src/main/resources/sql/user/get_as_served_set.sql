SELECT p.path AS selection_image_path 
FROM as_served_sets JOIN processed_images AS p ON selection_image_id = p.id
WHERE as_served_sets.id={id}