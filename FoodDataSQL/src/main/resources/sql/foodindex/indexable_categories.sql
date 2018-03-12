SELECT code, COALESCE(t1.local_description, t2.local_description) AS local_description
FROM categories
  LEFT JOIN categories_local as t1 ON categories.code = t1.category_code AND t1.locale_id = {locale_id}
  LEFT JOIN categories_local as t2 ON categories.code = t2.category_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id})
WHERE (t1.local_description IS NOT NULL OR t2.local_description IS NOT NULL) AND NOT categories.is_hidden
