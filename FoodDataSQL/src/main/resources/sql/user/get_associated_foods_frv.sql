WITH v AS(
  SELECT (SELECT code FROM foods WHERE code = {food_code}) AS food_code, 
         (SELECT id FROM locales WHERE id = {locale_id}) AS locale_id
)
SELECT v.food_code, v.locale_id, associated_foods.id, associated_foods.locale_id AS af_locale_id, associated_food_code, associated_category_code, text, link_as_main, generic_name
  FROM v LEFT JOIN associated_foods 
    ON v.food_code = associated_foods.food_code 
    AND (v.locale_id = associated_foods.locale_id OR associated_foods.locale_id IN (SELECT prototype_locale_id FROM locales WHERE id = v.locale_id))
ORDER BY id