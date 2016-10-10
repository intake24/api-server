SELECT as_served_sets.id, description, weight, url
FROM as_served_sets JOIN as_served_images ON as_served_sets.id = as_served_set_id
WHERE as_served_sets.id = {id} 
ORDER BY as_served_images.weight
