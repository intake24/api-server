WITH RECURSIVE v AS(
  SELECT (SELECT code FROM foods WHERE code={food_code}) AS food_code,
         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
)
SELECT v.food_code, v.locale_id, fnm.nutrient_table_id, fnm.nutrient_table_record_id
  FROM v LEFT JOIN foods_nutrient_mapping AS fnm ON v.food_code=fnm.food_code AND v.locale_id=fnm.locale_id
  