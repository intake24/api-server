SELECT subcategory_code, COALESCE(t1.local_description, t2.local_description) AS local_description
	FROM categories_categories
	INNER JOIN categories
		ON categories.code = categories_categories.category_code
	LEFT JOIN categories_local as t1
		ON subcategory_code = t1.category_code AND t1.locale_id = {locale_id}
	LEFT JOIN categories_local as t2
		ON subcategory_code = t2.category_code AND t2.locale_id IN (SELECT prototype_locale_id FROM locale_prototypes WHERE locale_id = {locale_id}
	LEFT JOIN categories_restrictions
		ON subcategory_code = categories_restrictions.category_code
WHERE
categories_categories.category_code = {category_code}
AND (t1.local_description IS NOT NULL OR t2.local_description IS NOT NULL)
AND (categories_restrictions.locale_id = {locale_id} OR categories_restrictions.locale_id IS NULL)
ORDER BY local_description;
