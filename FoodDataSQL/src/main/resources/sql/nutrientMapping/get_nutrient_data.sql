SELECT food_code, locale_id, m.nutrient_table_id, n.nutrient_table_record_id, nutrient_type_id, units_per_100g
FROM foods_nutrient_mapping AS m
  JOIN nutrient_table_records_nutrients AS n ON n.nutrient_table_id = m.nutrient_table_id AND n.nutrient_table_record_id = m.nutrient_table_record_id
WHERE food_code IN({food_codes}) AND (locale_id={locale_id} OR locale_id IN(SELECT prototype_locale_id FROM locales WHERE id={locale_id}))