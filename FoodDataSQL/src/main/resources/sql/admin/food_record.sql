WITH v AS(
  SELECT (SELECT code FROM foods WHERE code={food_code}) AS food_code,
         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
)
SELECT v.food_code, v.locale_id, code, description, local_description, do_not_use, food_group_id, 
       same_as_before_option, ready_meal_option, reasonable_amount, foods.version as version, 
       foods_local.version as local_version 
  FROM v LEFT JOIN foods ON v.food_code=foods.code
         LEFT JOIN foods_attributes ON v.food_code=foods_attributes.food_code
         LEFT JOIN foods_local ON v.food_code=foods_local.food_code AND v.locale_id=foods_local.locale_id