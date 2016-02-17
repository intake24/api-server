SELECT foods_categories.food_code, COALESCE(t1.local_description, t2.local_description) AS local_description
	FROM categories
	LEFT JOIN categories_local
		ON categories.code = categories_local.category_code
	LEFT JOIN categories_restrictions
		ON categories.code = categories_restrictions.category_code
	LEFT JOIN foods_categories
		ON categories.code = foods_categories.category_code
	LEFT JOIN foods_local as t1
		ON foods_categories.food_code = t1.food_code AND t1.locale_id = {locale_id}
	LEFT JOIN foods_local as t2
		ON foods_categories.food_code = t2.food_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locale_prototypes WHERE locale_id = {locale_id})
	LEFT JOIN foods_restrictions
		ON foods_categories.food_code = foods_restrictions.food_code
WHERE
	categories.code = {category_code}
	AND (COALESCE(t1.do_not_use, t2.do_not_use) = false)
	AND (categories_restrictions.locale_id = {locale_id} OR categories_restrictions.locale_id IS NULL)
	AND (foods_restrictions.locale_id = {locale_id} OR foods_restrictions.locale_id IS NULL)
ORDER BY local_description;
