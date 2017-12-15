package uk.ac.ncl.openlab.intake24.systemsql.migrations

import java.sql.Connection

import anorm.SQL
import org.slf4j.Logger
import uk.ac.ncl.openlab.intake24.sql.migrations.{Migration, MigrationFailed}

/**
  * Created by Tim Osadchiy on 01/03/2017.
  */
object SeparateMeals extends Migration {

  val versionFrom = 69l
  val versionTo = 70l

  val description = "Make meals independent of survey submissions to allow personal meal tracking"

  def apply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = {

    val sqlQuery =
      """
        |-- Migrate meals
        |
        |CREATE TABLE meals (
        |  id   INTEGER PRIMARY KEY,
        |  date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
        |  name VARCHAR(128)
        |);
        |
        |ALTER TABLE survey_submission_meals
        |  ADD COLUMN meal_id INTEGER;
        |
        |ALTER TABLE survey_submission_meals
        |  ADD CONSTRAINT survey_submission_meals_meal_id_fk
        |FOREIGN KEY (meal_id)
        |REFERENCES meals (id);
        |
        |ALTER TABLE survey_submission_meals
        |  ADD CONSTRAINT survey_submission_meals_meal_id_uq
        |UNIQUE (meal_id);
        |
        |WITH res AS (
        |  INSERT INTO meals (name, id)
        |    SELECT
        |      name,
        |      id
        |    FROM survey_submission_meals
        |  RETURNING id
        |) INSERT INTO survey_submission_meals (meal_id)
        |  SELECT id
        |  FROM res
        |  WHERE survey_submission_meals.id = res.id;
        |
        |ALTER TABLE survey_submission_meals
        |  ALTER COLUMN meal_id SET NOT NULL;
        |
        |ALTER TABLE survey_submission_foods
        |  DROP CONSTRAINT survey_submission_foods_survey_submission_id_fk;
        |ALTER TABLE public.survey_submission_foods
        |  ADD CONSTRAINT survey_submission_foods_meal_id_fk
        |FOREIGN KEY (meal_id) REFERENCES meals (id) ON DELETE CASCADE ON UPDATE CASCADE;
        |
        |CREATE SEQUENCE meals_id_seq;
        |ALTER TABLE meals
        |  ALTER COLUMN id SET DEFAULT nextval('meals_id_seq');
        |SELECT setval('meals_id_seq', (SELECT MAX(id)
        |                               FROM meals));
        |
        |-- Migrate foods
        |
        |ALTER TABLE survey_submission_meals
        |  DROP COLUMN name;
        |
        |ALTER TABLE survey_submission_foods RENAME TO meal_foods;
        |
        |ALTER TABLE meal_foods RENAME CONSTRAINT survey_submission_foods_pk TO meal_foods_pk;
        |ALTER TABLE meal_foods RENAME CONSTRAINT survey_submission_foods_meal_id_fk TO meal_foods_meal_id_fk;
        |ALTER TABLE meal_foods RENAME CONSTRAINT ssn_nutrient_type_id_fk TO food_nutrients_nutrient_type_id_fk;
        |
        |DROP INDEX survey_submission_nutrients_food_index RESTRICT;
        |CREATE INDEX food_nutrients_food_index ON food_nutrients (food_id);
        |
        |-- Migrate nutrients
        |
        |ALTER TABLE survey_submission_nutrients RENAME TO food_nutrients;
        |
        |ALTER TABLE food_nutrients RENAME CONSTRAINT survey_submission_nutrients_pk TO food_nutrients_pk;
        |ALTER TABLE food_nutrients RENAME CONSTRAINT survey_submission_nutrients_food_id_fk TO food_nutrients_food_id_fk;
        |
        |DROP INDEX survey_submission_nutrients_food_id_index RESTRICT;
        |CREATE INDEX meal_foods_meal_index ON meal_foods (meal_id);
      """.stripMargin

    SQL(sqlQuery).execute()

    Right(())

  }

  def unapply(logger: Logger)(implicit connection: Connection): Either[MigrationFailed, Unit] = ???

}