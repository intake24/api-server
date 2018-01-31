WITH v AS(
  SELECT (SELECT code FROM foods WHERE code={food_code}) AS food_code,
         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
)
SELECT 
  v.food_code, v.locale_id, psm.id, psm.method, psm.description, COALESCE(pi.path, psm.image_url) AS image_url, psm.use_for_recipes,
       conversion_factor, par.id as param_id, par.name as param_name, par.value as param_value
       FROM v LEFT JOIN foods_portion_size_methods AS psm ON psm.food_code=v.food_code AND psm.locale_id=v.locale_id
              LEFT JOIN foods_portion_size_method_params AS par ON psm.id = par.portion_size_method_id
              LEFT JOIN as_served_sets AS ass ON (psm.method='as-served' AND (par.name='serving-image-set' OR par.name='leftovers-image-set') AND par.value = ass.id)
              LEFT JOIN guide_images AS gi ON (psm.method='guide-image' AND par.name='guide-image-id' AND par.value=gi.id)
              LEFT JOIN processed_images AS pi ON ass.selection_image_id = pi.id OR gi.selection_image_id = pi.id
ORDER BY param_id
