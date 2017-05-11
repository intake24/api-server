SELECT code, description, local_description, simple_local_description,
  CASE WHEN do_not_use IS NULL THEN false ELSE do_not_use END AS do_not_use,
  ARRAY(SELECT locale_id FROM foods_restrictions WHERE food_code = code) AS restrict
FROM foods LEFT JOIN foods_local ON foods.code = foods_local.food_code
WHERE (lower(simple_local_description) LIKE {pattern} OR lower(description) LIKE {pattern} OR lower(code) LIKE {pattern}) AND foods_local.locale_id = {locale_id}
ORDER BY local_description DESC
LIMIT 30