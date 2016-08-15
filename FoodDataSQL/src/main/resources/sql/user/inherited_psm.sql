WITH RECURSIVE v AS(
  SELECT (SELECT code FROM foods WHERE code='DRNK') AS food_code,
         (SELECT id FROM locales WHERE id='en_GB') AS locale_id
), t(code, level) AS (
  (SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code IN (SELECT food_code FROM v) ORDER BY code)
    UNION
  (SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
)
SELECT v.food_code, v.locale_id, cpsm.id, cpsm.category_code, cpsm.method, cpsm.description, cpsm.image_url, cpsm.use_for_recipes,
       par.id as param_id, par.name as param_name, par.value as param_value, t.level
  FROM v CROSS JOIN t
    LEFT JOIN categories_portion_size_methods AS cpsm ON t.code=cpsm.category_code AND v.locale_id=cpsm.locale_id
    LEFT JOIN categories_portion_size_method_params AS par ON cpsm.id = par.portion_size_method_id
UNION ALL
    SELECT v.food_code, v.locale_id, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL FROM v WHERE NOT EXISTS (SELECT 1 FROM t LIMIT 1)
ORDER BY level, id, param_id