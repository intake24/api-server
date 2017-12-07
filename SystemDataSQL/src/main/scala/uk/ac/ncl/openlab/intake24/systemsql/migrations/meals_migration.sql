CREATE TABLE meals (
  id   SERIAL PRIMARY KEY,
  date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  name VARCHAR(128)
);

ALTER TABLE survey_submission_meals
  ADD COLUMN meal_id INTEGER;

ALTER TABLE survey_submission_meals
  ADD CONSTRAINT survey_submission_meals_meal_id_fk
FOREIGN KEY (meal_id)
REFERENCES meals (id);

ALTER TABLE survey_submission_meals
  ADD CONSTRAINT survey_submission_meals_meal_id_uq
UNIQUE (meal_id);

WITH res AS (
  INSERT INTO meals (name)
    SELECT name
    FROM survey_submission_meals
  RETURNING meals.id, survey_submission_meals.id AS survey_submission_meals_id
) INSERT INTO survey_submission_meals (meal_id)
  SELECT id FROM res WHERE survey_submission_meals.id = res.survey_submission_meals_id;

ALTER TABLE survey_submission_meals ALTER COLUMN meal_id SET NOT NULL;