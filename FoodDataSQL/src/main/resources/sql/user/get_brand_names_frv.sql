WITH v AS ( 
  SELECT (SELECT code FROM foods WHERE code={food_code}) AS food_code, 
         (SELECT id FROM locales WHERE id={locale_id}) AS locale_id
)
SELECT v.food_code, v.locale_id, brands.name FROM v LEFT JOIN brands ON v.food_code = brands.food_code AND v.locale_id = brands.locale_id
ORDER BY brands.id
