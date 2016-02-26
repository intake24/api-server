SELECT COUNT(*)
	FROM categories
	LEFT JOIN categories_restrictions
		ON categories.code = categories_restrictions.category_code
	LEFT JOIN categories_local as cl1
		ON categories.code = cl1.category_code AND cl1.locale_id = {locale_id}
	LEFT JOIN categories_local as cl2
		ON categories.code = cl2.category_code AND cl2.locale_id IN (SELECT prototype_locale_id FROM locale_prototypes WHERE locale_id = {locale_id})
WHERE
	categories.code = {category_code}
	AND (categories_restrictions.locale_id = {locale_id} OR categories_restrictions.locale_id IS NULL)
	AND (COALESCE(cl1.do_not_use, cl2.do_not_use) = false)
	AND (COALESCE(cl1.local_description, cl2.local_description) IS NOT NULL);
