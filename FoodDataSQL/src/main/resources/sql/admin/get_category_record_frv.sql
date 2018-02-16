WITH v AS(
  SELECT (SELECT code FROM categories WHERE code={category_code}) AS category_code,
         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
)
SELECT v.category_code, v.locale_id, code, description, local_description, is_hidden, 
       same_as_before_option, ready_meal_option, reasonable_amount, use_in_recipes, categories.version as version,
       categories_local.version as local_version 
FROM v
  LEFT JOIN categories ON v.category_code=categories.code
  LEFT JOIN categories_attributes ON v.category_code=categories_attributes.category_code
  LEFT JOIN categories_local ON v.category_code=categories_local.category_code AND v.locale_id=categories_local.locale_id