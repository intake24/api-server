WITH v AS(
  SELECT (SELECT code FROM categories WHERE code={category_code}) AS category_code
)
SELECT v.category_code, code
  FROM v LEFT JOIN foods_categories ON foods_categories.category_code = v.category_code
         LEFT JOIN foods ON foods.code = foods_categories.food_code 
