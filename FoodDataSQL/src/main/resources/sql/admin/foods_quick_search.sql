SELECT code, description, local_description, do_not_use
FROM foods LEFT JOIN foods_local ON foods.code = foods_local.food_code
WHERE (lower(local_description) LIKE {pattern} OR lower(description) LIKE {pattern} OR lower(code) LIKE {pattern}) AND foods_local.locale_id = {locale_id}
ORDER BY local_description DESC
LIMIT 30