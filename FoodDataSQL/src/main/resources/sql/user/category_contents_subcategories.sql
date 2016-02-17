SELECT subcategory_code, COALESCE(t1.local_description, t2.local_description) AS local_description
	FROM categories as c1
	LEFT JOIN categories_local as cl
		ON c1.code = cl.category_code
	LEFT JOIN categories_restrictions as r1
		ON c1.code = r1.category_code
	LEFT JOIN categories_categories
		ON categories_categories.category_code = c1.code
	LEFT JOIN categories as c2
		ON subcategory_code = c2.code
	LEFT JOIN categories_local as t1
		ON subcategory_code = t1.category_code AND t1.locale_id = {locale_id}
	LEFT JOIN categories_local as t2
		ON subcategory_code = t2.category_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locale_prototypes WHERE locale_id = {locale_id})
	LEFT JOIN categories_restrictions as r2
		ON subcategory_code = r2.category_code
WHERE
	c1.code = {category_code}
	AND (c1.is_hidden = false)
	AND (c2.is_hidden = false)
	AND (COALESCE(t1.do_not_use, t2.do_not_use) = false)
	AND (r1.locale_id = {locale_id} OR r1.locale_id IS NULL)
	AND (r2.locale_id = {locale_id} OR r2.locale_id IS NULL)
ORDER BY local_description;
