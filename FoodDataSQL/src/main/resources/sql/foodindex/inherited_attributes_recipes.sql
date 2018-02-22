WITH RECURSIVE fc(food_code) AS (
    SELECT code FROM foods AS food_code
), t(food_code, code, level) AS (
  (SELECT food_code, category_code as code, 1 as level FROM foods_categories WHERE food_code IN (SELECT food_code FROM fc) ORDER BY code)
  UNION
  (SELECT food_code, category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
), u(food_code, level, use_in_recipes) AS (
  (SELECT food_code, 0, use_in_recipes as code FROM foods_attributes WHERE food_code IN (SELECT food_code FROM fc))
  UNION ALL
  (SELECT food_code, level, use_in_recipes FROM categories_attributes JOIN t ON code = category_code ORDER BY level)
  UNION ALL
  (SELECT food_code, 10000000, use_in_recipes FROM attribute_defaults CROSS JOIN fc)
)
SELECT food_code, (array_agg(use_in_recipes ORDER BY use_in_recipes IS NULL, level ))[1] AS use_in_recipes
FROM u
GROUP BY food_code
ORDER BY food_code
