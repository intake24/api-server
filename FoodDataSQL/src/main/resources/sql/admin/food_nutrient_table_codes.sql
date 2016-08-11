WITH v AS(
  SELECT (SELECT code FROM foods WHERE code={food_code}) AS food_code,
         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
)
SELECT v.food_code, v.locale_id, nutrient_table_id, nutrient_table_record_id 
  FROM v LEFT JOIN foods_nutrient_mapping 
    ON v.food_code=foods_nutrient_mapping.food_code AND v.locale_id = foods_nutrient_mapping.locale_id