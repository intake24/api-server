SELECT categories_categories.subcategory_code as subcategory_code, COALESCE(cl1.local_description, cl2.local_description) AS local_description
	FROM categories_categories
	LEFT JOIN categories_local as cl1
		ON cl1.category_code = categories_categories.subcategory_code AND cl1.locale_id = {locale_id}
	LEFT JOIN categories_local as cl2
		ON cl2.category_code = categories_categories.subcategory_code AND cl2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = {locale_id})
	LEFT JOIN categories_restrictions
		ON categories_restrictions.category_code = categories_categories.subcategory_code
WHERE
	categories_categories.category_code = {category_code}
	AND (COALESCE(cl1.local_description, cl2.local_description) IS NOT NULL)
	AND (COALESCE(cl1.do_not_use, cl2.do_not_use) = false)
	AND (categories_restrictions.locale_id = {locale_id} OR categories_restrictions.locale_id IS NULL)
ORDER BY local_description;
