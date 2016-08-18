SELECT associated_food_code, f1.description as food_english_description, foods_local.local_description as food_local_description, foods_local.do_not_use as food_do_not_use,
  associated_category_code, c1.description as category_english_description, c1.is_hidden as category_is_hidden, categories_local.local_description as category_local_description, 
  text, link_as_main, generic_name
FROM associated_foods
  LEFT JOIN foods as f1 ON associated_foods.associated_food_code = f1.code
  LEFT JOIN foods_local ON associated_foods.associated_food_code = foods_local.food_code AND foods_local.locale_id = {locale_id}
  LEFT JOIN categories as c1 ON associated_foods.associated_category_code = c1.code
  LEFT JOIN categories_local ON associated_foods.associated_category_code = categories_local.category_code AND categories_local.locale_id = {locale_id}
WHERE
  associated_foods.food_code = {food_code} AND associated_foods.locale_id = {locale_id}
ORDER BY id
