WITH v AS(
  SELECT (SELECT code FROM categories WHERE code={category_code}) AS category_code,
         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
), t AS(
 SELECT v.locale_id, v.category_code, subcategory_code AS code, description, COALESCE(cl1.local_description, cl2.local_description) AS local_description, is_hidden
   FROM v LEFT JOIN categories_categories ON categories_categories.category_code = v.category_code
          LEFT JOIN categories ON categories.code = categories_categories.subcategory_code
          LEFT JOIN categories_local AS cl1 ON cl1.category_code=categories_categories.subcategory_code AND cl1.locale_id = v.locale_id
          LEFT JOIN categories_local AS cl2 ON cl2.category_code=categories_categories.subcategory_code AND cl2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id=v.locale_id)
  WHERE NOT is_hidden
)
SELECT locale_id, category_code, code, description, local_description FROM t
UNION ALL
SELECT locale_id, category_code, NULL, NULL, NULL FROM v WHERE NOT EXISTS (SELECT 1 FROM t LIMIT 1)
ORDER BY local_description, description
