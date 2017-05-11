WITH v AS(
  SELECT (SELECT code FROM categories WHERE code={category_code}) AS category_code,
  (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
)
SELECT v.locale_id, v.category_code, code, description, local_description,
       CASE WHEN do_not_use IS NULL THEN false ELSE do_not_use END AS do_not_use,
       ARRAY(SELECT locale_id FROM foods_restrictions WHERE food_code = code) AS restrict
  FROM v LEFT JOIN foods_categories ON foods_categories.category_code = v.category_code
         LEFT JOIN foods ON foods.code = foods_categories.food_code 
         LEFT JOIN foods_local ON foods.code = foods_local.food_code AND foods_local.locale_id = v.locale_id
ORDER BY local_description