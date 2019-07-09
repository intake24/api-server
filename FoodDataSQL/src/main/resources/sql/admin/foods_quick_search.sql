WITH p AS (
    SELECT prototype_locale_id AS l FROM locales WHERE id={locale_id} UNION SELECT {locale_id} AS l
)
SELECT code, description, coalesce(fl.local_description, flp.local_description) as local_description, coalesce(fl.simple_local_description, flp.simple_local_description) as simple_local_description
FROM foods_local_lists
    LEFT JOIN foods ON foods.code = foods_local_lists.food_code
    LEFT JOIN foods_local as fl ON fl.food_code = foods_local_lists.food_code AND fl.locale_id = {locale_id}
    LEFT JOIN foods_local as flp ON flp.food_code = foods_local_lists.food_code AND flp.locale_id IN (SELECT prototype_locale_id AS l FROM locales WHERE id={locale_id})
WHERE foods_local_lists.locale_id = {locale_id} AND (lower(code) LIKE {pattern} OR lower(coalesce(fl.simple_local_description, flp.simple_local_description)) LIKE {pattern})
ORDER BY local_description DESC
LIMIT 30