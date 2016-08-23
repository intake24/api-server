SELECT code, description, local_description, is_hidden
FROM categories LEFT JOIN categories_local ON categories.code = categories_local.category_code
WHERE (lower(local_description) LIKE {pattern} OR lower(description) LIKE {pattern} OR lower(code) LIKE {pattern}) AND categories_local.locale_id = {locale_id}
ORDER BY local_description DESC
LIMIT 30