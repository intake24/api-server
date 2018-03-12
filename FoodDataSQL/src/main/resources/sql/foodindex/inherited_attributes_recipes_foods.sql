WITH RECURSIVE t(food_code, parent_category_code, level) AS (
  (SELECT foods.code AS food_code, foods_categories.category_code AS parent_category_code, 1 as level
   FROM foods JOIN foods_categories ON foods.code = foods_categories.food_code ORDER BY foods.code)
  UNION
  (SELECT t.food_code, categories_categories.category_code AS parent_category_code, level + 1 as level
   FROM t JOIN categories_categories ON t.parent_category_code = categories_categories.subcategory_code ORDER BY t.parent_category_code)
), u(food_code, level, use_in_recipes) AS (
  (SELECT food_code, 0, use_in_recipes as code FROM foods JOIN foods_attributes ON foods.code = foods_attributes.food_code)
  UNION ALL
  (SELECT t.food_code, level, use_in_recipes FROM t JOIN categories_attributes ON t.parent_category_code = categories_attributes.category_code)
  UNION ALL
  (SELECT foods.code AS food_code, 10000000, use_in_recipes FROM attribute_defaults CROSS JOIN foods)
)
SELECT food_code AS code, (array_agg(use_in_recipes ORDER BY use_in_recipes IS NULL, level ))[1] AS use_in_recipes
FROM u
GROUP BY food_code
ORDER BY food_code
