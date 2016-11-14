WITH v AS(
  SELECT (SELECT code FROM categories WHERE code={category_code}) AS category_code,
         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
)
SELECT v.locale_id, v.category_code, subcategory_code AS code, description, local_description, is_hidden
  FROM v LEFT JOIN categories_categories ON categories_categories.category_code = v.category_code
         LEFT JOIN categories ON categories.code = categories_categories.subcategory_code 
         LEFT JOIN categories_local AS cl1 ON cl1.category_code=categories_categories.subcategory_code AND cl1.locale_id = v.locale_id         
ORDER BY local_description
