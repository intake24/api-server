SELECT f.id AS food_id, n.nutrient_type_id as n_type, n.amount as n_amount
FROM survey_submission_meals AS m
  JOIN survey_submission_foods AS f ON f.meal_id = m.id
  JOIN survey_submission_nutrients AS n ON n.food_id = f.id
WHERE m.survey_submission_id IN({submission_ids})