WITH RECURSIVE t(category_code, parent_category_code, level) AS (
  (SELECT categories.code AS category_code, categories_categories.category_code AS parent_category_code, 1 as level
   FROM categories JOIN categories_categories ON categories.code = categories_categories.subcategory_code ORDER BY categories.code)
  UNION
  (SELECT t.category_code, categories_categories.category_code AS parent_category_code, level + 1 as level
   FROM t JOIN categories_categories ON t.parent_category_code = categories_categories.subcategory_code ORDER BY t.parent_category_code)
), u(category_code, level, use_in_recipes) AS (
  (SELECT category_code, 0, use_in_recipes as code FROM categories JOIN categories_attributes ON categories.code = categories_attributes.category_code)
  UNION ALL
  (SELECT t.category_code, level, use_in_recipes FROM t JOIN categories_attributes ON t.parent_category_code = categories_attributes.category_code)
  UNION ALL
  (SELECT categories.code AS category_code, 10000000, use_in_recipes FROM attribute_defaults CROSS JOIN categories)
)
SELECT category_code AS code, (array_agg(use_in_recipes ORDER BY use_in_recipes IS NULL, level ))[1] AS use_in_recipes
FROM u
GROUP BY category_code
ORDER BY category_code
