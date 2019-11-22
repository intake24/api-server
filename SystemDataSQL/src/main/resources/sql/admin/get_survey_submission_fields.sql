SELECT foods.id AS food_id, fields.field_name, fields.value
FROM survey_submission_meals AS m
  JOIN survey_submission_foods AS foods ON foods.meal_id = m.id
  JOIN survey_submission_fields AS fields ON fields.food_id = foods.id
WHERE m.survey_submission_id IN({submission_ids})
