WITH RECURSIVE t(code, level) AS (
  (SELECT category_code as code, 0 as level FROM categories_categories WHERE subcategory_code={category_code} ORDER BY code)
    UNION ALL
  (SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
)
SELECT DISTINCT ON(code) (SELECT code FROM categories WHERE code={category_code}) AS category_code, code, level FROM t
  UNION ALL
SELECT (SELECT code FROM categories WHERE code={category_code}), NULL, NULL WHERE NOT EXISTS(SELECT 1 FROM categories_categories WHERE subcategory_code={category_code})
ORDER BY level