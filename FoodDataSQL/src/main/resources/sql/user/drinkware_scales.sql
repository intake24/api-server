SELECT drinkware_sets.id, drinkware_scales.id as scale_id, description, guide_image_id, 
  width, height, empty_level, full_level, choice_id, base_image_url, overlay_image_url
FROM drinkware_sets JOIN drinkware_scales ON drinkware_set_id = drinkware_sets.id
WHERE drinkware_sets.id = {drinkware_id}
ORDER by scale_id
