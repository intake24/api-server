WITH RECURSIVE t(food_code, code, level) AS (
  (
    SELECT
      food_code,
      category_code AS code,
      0             AS level
    FROM foods_categories
    WHERE food_code = {food_code}
    ORDER BY code
  )
  UNION ALL
  (SELECT
     food_code,
     category_code AS code,
     level + 1     AS level
   FROM t
     JOIN categories_categories ON subcategory_code = code
   ORDER BY code)
)
SELECT DISTINCT ON (t.code)
  t.food_code,
  t.code,
  categories.description,
  categories_local.local_description,
  t.level,
  categories.is_hidden
FROM t
  JOIN categories ON categories.code = t.code
  JOIN categories_local ON categories_local.category_code = t.code
WHERE locale_id = {locale_id}