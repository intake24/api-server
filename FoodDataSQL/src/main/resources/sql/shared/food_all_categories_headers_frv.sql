WITH RECURSIVE t(code, level) AS (
  (SELECT category_code as code, 0 as level FROM foods_categories WHERE food_code = {food_code} ORDER BY code)
    UNION ALL
  (SELECT category_code as code, level + 1 as level FROM t JOIN categories_categories ON subcategory_code = code ORDER BY code)
)
SELECT DISTINCT ON(categories.code) (SELECT code FROM foods WHERE code={food_code}) as food_code, (SELECT id FROM locales WHERE id={locale_id}) AS locale_id, categories.code, description, local_description, is_hidden 
FROM t 
  JOIN categories on t.code = categories.code
  LEFT JOIN categories_local on t.code = categories_local.category_code AND categories_local.locale_id = {locale_id}
UNION ALL
SELECT (SELECT code FROM foods WHERE code={food_code}), (SELECT id FROM locales WHERE id={locale_id}), NULL, NULL, NULL, NULL WHERE NOT EXISTS(SELECT 1 FROM foods_categories WHERE food_code={food_code})