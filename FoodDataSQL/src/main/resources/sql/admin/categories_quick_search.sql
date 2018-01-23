WITH p AS (
    SELECT prototype_locale_id AS l FROM locales WHERE id={locale_id} UNION SELECT {locale_id} AS l
)
SELECT code, description, local_description, simple_local_description, locale_id, is_hidden
FROM categories LEFT JOIN categories_local ON categories.code = categories_local.category_code
WHERE (lower(code) LIKE {pattern} OR lower(simple_local_description) LIKE {pattern}) AND categories_local.locale_id IN (SELECT l FROM p)
ORDER BY local_description DESC
LIMIT 30
