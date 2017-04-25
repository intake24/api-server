SELECT
    mf.meal_id,mf.name,mf.brand,mf.description,mf.portion_size,mf.leftovers
FROM survey_submission_missing_foods AS mf JOIN survey_submission_meals AS m ON m.id = mf.meal_id
WHERE m.survey_submission_id IN ({submission_ids})
ORDER BY mf.id
