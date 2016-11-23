SELECT lnt.nutrient_type_id, nt.description, nu.symbol
FROM local_nutrient_types AS lnt
  JOIN nutrient_types AS nt ON lnt.nutrient_type_id = nt.id
  JOIN nutrient_units AS nu ON nt.unit_id = nu.id
WHERE locale_id={locale_id} 
ORDER BY lnt.id
