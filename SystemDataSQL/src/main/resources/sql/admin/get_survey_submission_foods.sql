SELECT m.id as meal_id, f.id as food_id, code, english_description, local_description, ready_meal, search_term, portion_size_method_id,
    reasonable_amount, food_group_id, brand, nutrient_table_id, nutrient_table_code,
    ARRAY(SELECT ARRAY[name,value] FROM survey_submission_food_custom_fields AS cf WHERE cf.food_id = f.id) AS custom_fields,
    ARRAY(SELECT ARRAY[name,value] FROM survey_submission_portion_size_fields AS psf WHERE psf.food_id = f.id) AS portion_size_data
FROM
    survey_submission_meals AS m JOIN survey_submission_foods AS f ON f.meal_id = m.id
WHERE m.survey_submission_id IN ({submission_ids})
ORDER BY f.id