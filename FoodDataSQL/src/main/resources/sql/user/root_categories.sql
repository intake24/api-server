WITH v AS(
  SELECT (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
), t AS(
  SELECT DISTINCT ON (code) c.code, c.description, c.is_hidden
    FROM categories AS c
      LEFT JOIN categories_categories AS cc1 ON c.code=cc1.subcategory_code
         WHERE (cc1.category_code IS NULL OR NOT EXISTS(SELECT is_hidden FROM categories_categories AS cc2 INNER JOIN categories AS c2 ON cc2.category_code=c2.code WHERE NOT is_hidden AND cc2.subcategory_code=c.code))
                AND (NOT c.is_hidden)
)
SELECT v.locale_id, t.code, t.description, COALESCE(cl1.local_description, cl2.local_description) as local_description FROM 
  v CROSS JOIN t 
    LEFT JOIN categories_local as cl1 ON t.code=cl1.category_code AND v.locale_id=cl1.locale_id
    LEFT JOIN categories_local as cl2 ON t.code=cl2.category_code AND cl2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id=v.locale_id)
UNION ALL
  SELECT locale_id, NULL, NULL, NULL FROM v WHERE NOT EXISTS(SELECT 1 FROM t LIMIT 1)
ORDER BY local_description, description
