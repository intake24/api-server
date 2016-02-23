SELECT foods_categories.food_code, COALESCE(fl1.local_description, fl2.local_description) AS local_description
	FROM categories
	LEFT JOIN categories_restrictions
		ON categories.code = categories_restrictions.category_code
	LEFT JOIN categories_local as t1
		ON categories.code = t1.category_code AND t1.locale_id = 'test1'
	LEFT JOIN categories_local as t2
		ON categories.code = t2.category_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locale_prototypes WHERE locale_id = 'test1')
	LEFT JOIN foods_categories
		ON categories.code = foods_categories.category_code
	LEFT JOIN foods_local as fl1
		ON fl1.food_code = foods_categories.food_code AND fl1.locale_id = 'test1' AND fl1.local_description IS NOT NULL AND fl1.do_not_use = false
	LEFT JOIN foods_local as fl2
		ON fl2.food_code = foods_categories.food_code AND fl2.locale_id IN (SELECT prototype_locale_id FROM locale_prototypes WHERE locale_id = 'test1') AND fl2.local_description IS NOT NULL AND fl2.do_not_use = false
	LEFT JOIN foods_restrictions
		ON foods_restrictions.food_code = foods_categories.food_code
WHERE
	categories.code = 'C005'
	AND (COALESCE(t1.local_description, t2.local_description) IS NOT NULL)
	AND (COALESCE(t1.do_not_use, t2.do_not_use) = false)
	AND (categories_restrictions.locale_id = 'test1' OR categories_restrictions.locale_id IS NULL)
	AND (foods_restrictions.locale_id = 'test1' OR foods_restrictions.locale_id IS NULL)
