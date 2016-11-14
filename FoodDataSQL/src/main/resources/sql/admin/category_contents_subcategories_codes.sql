WITH v AS(
  SELECT (SELECT code FROM categories WHERE code={category_code}) AS category_code
)
SELECT v.category_code, subcategory_code AS code
  FROM v LEFT JOIN categories_categories ON categories_categories.category_code = v.category_code
         LEFT JOIN categories ON categories.code = categories_categories.subcategory_code 
