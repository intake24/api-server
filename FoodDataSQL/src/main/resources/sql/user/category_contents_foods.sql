SELECT code, COALESCE(t1.local_description, t2.local_description) AS local_description
	FROM foods_categories
	INNER JOIN foods
		ON foods_categories.food_code = foods_categories.food_code
	LEFT JOIN foods_local as t1
		ON foods_categories.food_code = t1.food_code AND t1.locale_id = {locale_id}
	LEFT JOIN foods_local as t2
		ON foods_categories.food_code = t2.food_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locale_prototypes WHERE locale_id = {locale_id})
	LEFT JOIN foods_restrictions
		ON foods_categories.food_code = foods_restrictions.food_code
WHERE
	foods_categories.category_code = {category_code}
	AND (t1.local_description IS NOT NULL OR t2.local_description IS NOT NULL)
	AND (foods_restrictions.locale_id = {locale_id} OR foods_restrictions.locale_id IS NULL)
ORDER BY local_description;
