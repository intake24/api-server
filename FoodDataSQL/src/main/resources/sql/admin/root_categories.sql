WITH v AS(
  SELECT (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
), t AS(
  SELECT DISTINCT c.code, c.description, c.is_hidden 
    FROM categories AS c 
      LEFT JOIN categories_categories AS cc1 ON c.code=cc1.subcategory_code
  WHERE cc1.category_code IS NULL OR NOT EXISTS(SELECT is_hidden FROM categories_categories AS cc2 INNER JOIN categories AS c2 ON cc2.category_code=c2.code WHERE NOT is_hidden AND cc2.subcategory_code=c.code)
  )
SELECT v.locale_id, t.code, t.description, t.is_hidden, local_description FROM v CROSS JOIN t LEFT JOIN categories_local ON v.locale_id = categories_local.locale_id AND t.code=categories_local.category_code
  UNION ALL
SELECT v.locale_id, NULL, NULL, NULL, NULL FROM v WHERE NOT EXISTS(SELECT 1 FROM t)
ORDER BY local_description
