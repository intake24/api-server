SELECT code, description, coalesce(fl.local_description, flp.local_description) AS local_description
FROM foods_categories
    JOIN foods_local_lists ON foods_local_lists.food_code = foods_categories.food_code AND foods_local_lists.locale_id = {locale_id}
    LEFT JOIN foods ON foods.code = foods_categories.food_code
    LEFT JOIN foods_local as fl ON fl.food_code = foods_categories.food_code AND fl.locale_id = {locale_id}
    LEFT JOIN foods_local as flp ON flp.food_code = foods_categories.food_code AND flp.locale_id IN (SELECT prototype_locale_id AS l FROM locales WHERE id={locale_id})
WHERE foods_categories.category_code = {category_code}
ORDER BY coalesce(fl.local_description, flp.local_description)