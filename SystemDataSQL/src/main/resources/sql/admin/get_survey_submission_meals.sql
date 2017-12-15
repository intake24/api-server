SELECT m.survey_submission_id AS submission_id, m.id AS meal_id, m.hours, m.minutes, mm.name,
                               ARRAY(SELECT ARRAY[name, value] FROM survey_submission_meal_custom_fields AS mcf WHERE mcf.meal_id = m.id) AS custom_fields
FROM survey_submission_meals AS m
JOIN meals AS mm ON m.meal_id = mm.id
WHERE m.survey_submission_id IN({submission_ids})