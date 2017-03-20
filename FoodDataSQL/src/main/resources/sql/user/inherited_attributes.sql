WITH RECURSIVE v AS(
  SELECT (SELECT code FROM foods WHERE code={food_code}) AS food_code
), t(code, level) AS (
  (SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code IN (SELECT food_code FROM v) ORDER BY code)
    UNION
  (SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
), u AS (
  (SELECT same_as_before_option, ready_meal_option, reasonable_amount, true as is_food_record, food_code as code FROM foods_attributes WHERE food_code IN (SELECT food_code FROM v))
    UNION ALL
  (SELECT same_as_before_option, ready_meal_option, reasonable_amount, false as is_food_record, category_code as code FROM categories_attributes JOIN t ON code = category_code ORDER BY level)
    UNION ALL
  (SELECT same_as_before_option, ready_meal_option, reasonable_amount, false as is_food_record, NULL as code FROM attribute_defaults LIMIT 1)
)
SELECT v.food_code, u.same_as_before_option, u.ready_meal_option, u.reasonable_amount, u.is_food_record, u.code 
  FROM v CROSS JOIN u
