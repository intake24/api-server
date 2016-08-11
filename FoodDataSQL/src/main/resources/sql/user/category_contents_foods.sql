WITH v AS(
  SELECT (SELECT code FROM categories WHERE code={category_code}) AS category_code,
  (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
), t AS(
  SELECT v.locale_id, v.category_code, code, description, COALESCE(fl1.local_description, fl2.local_description) AS local_description, COALESCE(fl1.do_not_use, fl2.do_not_use) AS do_not_use
    FROM v LEFT JOIN foods_categories ON foods_categories.category_code = v.category_code
           LEFT JOIN foods ON foods.code = foods_categories.food_code 
           LEFT JOIN foods_local as fl1 ON foods.code = fl1.food_code AND fl1.locale_id = v.locale_id
           LEFT JOIN foods_local as fl2 ON foods.code = fl2.food_code AND fl2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id=v.locale_id)
  WHERE NOT COALESCE(fl1.do_not_use, fl2.do_not_use) 
)
SELECT locale_id, category_code, code, description, local_description FROM t
UNION ALL
SELECT locale_id, category_code, NULL, NULL, NULL FROM v WHERE NOT EXISTS(SELECT 1 FROM t LIMIT 1)
ORDER BY local_description, description
