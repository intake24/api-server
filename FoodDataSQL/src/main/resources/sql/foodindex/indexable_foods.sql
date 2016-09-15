SELECT code, COALESCE(t1.local_description, t2.local_description) AS local_description
FROM foods
  LEFT JOIN foods_local as t1 ON foods.code = t1.food_code AND t1.locale_id = {locale_id}
  LEFT JOIN foods_local as t2 ON foods.code = t2.food_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id})
  LEFT JOIN foods_restrictions ON foods.code = foods_restrictions.food_code
WHERE 
  (t1.local_description IS NOT NULL OR t2.local_description IS NOT NULL)
  AND NOT (t1.do_not_use OR (t2.do_not_use IS NOT NULL AND t2.do_not_use))
  AND (foods_restrictions.locale_id = {locale_id} OR foods_restrictions.locale_id IS NULL)
