WITH RECURSIVE fc(food_code) AS (
    SELECT code FROM foods AS food_code
), t(food_code, code, level) AS (
  (SELECT food_code, category_code as code, 0 as level FROM foods_categories WHERE food_code IN (SELECT food_code FROM fc) ORDER BY code)
  UNION
  (SELECT food_code, category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
), u AS (
  (SELECT food_code, same_as_before_option, ready_meal_option, reasonable_amount, use_in_recipes, food_code as code FROM foods_attributes WHERE food_code IN (SELECT food_code FROM fc))
  UNION ALL
  (SELECT food_code, same_as_before_option, ready_meal_option, reasonable_amount, use_in_recipes, category_code as code FROM categories_attributes JOIN t ON code = category_code ORDER BY level)
  UNION ALL
  (SELECT food_code, same_as_before_option, ready_meal_option, reasonable_amount, use_in_recipes, NULL as code FROM attribute_defaults CROSS JOIN fc)
)
SELECT food_code,
  (array_agg(same_as_before_option ORDER BY same_as_before_option IS NULL))[1] AS same_as_before_option,
  (array_agg(ready_meal_option ORDER BY ready_meal_option IS NULL))[1] AS ready_meal_option,
  (array_agg(reasonable_amount ORDER BY reasonable_amount IS NULL))[1] AS reasonable_amount,
  (array_agg(use_in_recipes ORDER BY use_in_recipes IS NULL))[1] AS use_in_recipes
FROM u
GROUP BY food_code
ORDER BY food_code
