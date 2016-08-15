WITH RECURSIVE v AS(
  SELECT (SELECT code FROM foods WHERE code='COCO') AS food_code,
         (SELECT id FROM locales WHERE id='en_GB') AS locale_id
)
SELECT v.food_code, v.locale_id, foods.description as english_description, fl1.local_description as local_description, fl2.local_description AS prototype_description, food_group_id
  FROM v LEFT JOIN foods ON v.food_code=foods.code
         LEFT JOIN foods_local as fl1 ON v.food_code=fl1.food_code AND v.locale_id=fl1.locale_id
         LEFT JOIN foods_local as fl2 ON v.food_code=fl2.food_code AND fl2.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = v.locale_id)
