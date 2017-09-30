WITH sel AS (
    SELECT
      survey_submission_meals.id                                        AS meal_id,
      ARRAY_AGG(DISTINCT survey_submission_foods.code)                  AS foods,
      ARRAY_LENGTH(ARRAY_AGG(DISTINCT survey_submission_foods.code), 1) AS size
    FROM surveys
      JOIN survey_submissions ON surveys.id = survey_submissions.survey_id
      JOIN survey_submission_meals ON survey_submission_meals.survey_submission_id = survey_submissions.id
      JOIN survey_submission_foods ON survey_submission_foods.meal_id = survey_submission_meals.id
    GROUP BY survey_submission_meals.id
)
SELECT foods FROM sel WHERE size > 1;