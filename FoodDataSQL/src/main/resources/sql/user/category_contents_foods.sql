SELECT foods_categories.food_code as food_code, COALESCE(fl1.local_description, fl2.local_description) AS local_description
	FROM foods_categories
	LEFT JOIN foods_local as fl1
		ON fl1.food_code = foods_categories.food_code AND fl1.locale_id = {locale_id}
	LEFT JOIN foods_local as fl2
		ON fl2.food_code = foods_categories.food_code AND fl2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id})
	LEFT JOIN foods_restrictions
		ON foods_restrictions.food_code = foods_categories.food_code
WHERE
	foods_categories.category_code = {category_code}
	AND (COALESCE(fl1.local_description, fl2.local_description) IS NOT NULL)
	AND (COALESCE(fl1.do_not_use, fl2.do_not_use) = false)
	AND (foods_restrictions.locale_id = {locale_id} OR foods_restrictions.locale_id IS NULL)
ORDER BY local_description;
