/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.ncl.openlab.intake24.datastoresql

import java.sql.Connection
import anorm.NamedParameter
import anorm.BatchSql
import scala.collection.JavaConversions._

object Queries {


  val userCustomFieldsDelete = """DELETE from user_custom_fields WHERE survey_id = {survey_id} AND user_id = {user_id}"""

  val surveysInsert = """INSERT INTO surveys VALUES ({id}, {state}, {start_date}, {end_date}, {scheme_id}, {locale}, {allow_gen_users}, {suspension_reason}, {survey_monkey_url}, {support_email})"""

  val surveysInsertInit = """INSERT INTO surveys VALUES ({id}, 0, DEFAULT, DEFAULT, {scheme_id}, {locale}, {allow_gen_users}, '', {survey_monkey_url}, {support_email})"""

  val surveysSelect = """SELECT state, start_date, end_date, scheme_id, locale, allow_gen_users, suspension_reason, survey_monkey_url, support_email FROM surveys WHERE id = {survey_id}"""

  val surveysUpdate = """UPDATE surveys SET state={state}, start_date={start_date}, end_date={end_date}, scheme_id={scheme_id}, locale={locale}, allow_gen_users={allow_gen_users}, support_email={support_email}, suspension_reason={suspension_reason}, survey_monkey_url={survey_monkey_url} WHERE id = {survey_id}"""

  val surveySubmissionsInsert = """INSERT INTO survey_submissions VALUES ({id}::uuid, {survey_id}, {user_id}, {start_time}, {end_time}, {log})"""

  val surveyCustomFieldsInsert = """INSERT INTO survey_submission_custom_fields VALUES (DEFAULT, {survey_submission_id}::uuid, {name}, {value})"""

  val surveyUserCustomFieldsInsert = """INSERT INTO survey_submission_user_custom_fields VALUES (DEFAULT, {survey_submission_id}::uuid, {name}, {value})"""

  val surveyMealsInsert = """INSERT INTO survey_submission_meals VALUES (DEFAULT, {survey_submission_id}::uuid, {hours}, {minutes}, {name})"""

  val surveyMealsCustomFieldsInsert = """INSERT INTO survey_submission_meal_custom_fields VALUES (DEFAULT, {meal_id}, {name}, {value})"""

  val surveyFoodsInsert = """INSERT INTO survey_submission_foods VALUES (DEFAULT, {meal_id}, {code}, {english_description}, {local_description}, {ready_meal}, {search_term}, {portion_size_method_id}, {reasonable_amount},{food_group_id},{food_group_english_description},{food_group_local_description},{brand},{nutrient_table_id},{nutrient_table_code})"""

  val surveyFoodCustomFieldsInsert = """INSERT INTO survey_submission_food_custom_fields VALUES (DEFAULT, {food_id}, {name}, {value})"""

  val surveyFoodPortionSizeFieldsInsert = """INSERT INTO survey_submission_portion_size_fields VALUES (DEFAULT, {food_id}, {name}, {value})"""

  val surveyFoodNutrientValuesInsert = """INSERT INTO survey_submission_nutrients(id, food_id, nutrient_type_id, amount) VALUES (DEFAULT, {food_id}, {nutrient_type_id}, {value})"""

  val surveySubmissionsSelectByTime = """SELECT id, survey_id, user_id, start_time, end_time, log FROM survey_submissions WHERE survey_id={survey_id} AND start_time>{time_from} AND end_time<{time_to} ORDER BY end_time ASC"""

  val surveySubmissionCustomFieldsSelect = """SELECT name, value FROM survey_submission_custom_fields WHERE survey_submission_id = {survey_submission_id}::uuid"""

  val surveySubmissionUserCustomFieldsSelect = """SELECT name, value FROM survey_submission_user_custom_fields WHERE survey_submission_id = {survey_submission_id}::uuid"""

  val surveySubmissionMealsSelect =
    """|SELECT m.id as meal_id, m.hours, m.minutes, m.name, f.name as cf_name, f.value as cf_value
       |FROM survey_submissions s
       |JOIN survey_submission_meals m
       |ON s.id = m.survey_submission_id
       |LEFT JOIN survey_submission_meal_custom_fields f
       |ON f.meal_id = m.id
       |WHERE m.survey_submission_id = {survey_submission_id}::uuid""".stripMargin

  val surveySubmissionFoodsSelect =
    """|SELECT 
       |m.id as meal_id, f.id as food_id, code, english_description, local_description, ready_meal, search_term, portion_size_method_id, 
       |reasonable_amount, food_group_id, food_group_english_description, food_group_local_description, brand, nutrient_table_id, nutrient_table_code, cf.name as cf_name, cf.value as cf_value
       |FROM survey_submissions s
       |JOIN survey_submission_meals m
       |ON s.id = m.survey_submission_id
       |JOIN survey_submission_foods f
       |ON f.meal_id = m.id
       |LEFT JOIN survey_submission_food_custom_fields cf
       |ON cf.food_id = f.id
       |WHERE m.survey_submission_id = {survey_submission_id}::uuid""".stripMargin

  val surveySubmissionNutrientsSelect =
    """|SELECT f.id as food_id, n.nutrient_type_id as n_type, n.amount as n_amount
       |FROM survey_submissions s
       |JOIN survey_submission_meals m
       |ON s.id = m.survey_submission_id
       |JOIN survey_submission_foods f
       |ON f.meal_id = m.id
       |JOIN survey_submission_nutrients n
       |ON n.food_id = f.id
       |WHERE m.survey_submission_id = {survey_submission_id}::uuid""".stripMargin

  val surveySubmissionPortionSizeDataSelect =
    """|SELECT f.id as food_id, ps.name, value
       |FROM survey_submissions s
       |JOIN survey_submission_meals m
       |ON s.id = m.survey_submission_id
       |JOIN survey_submission_foods f
       |ON f.meal_id = m.id
       |JOIN survey_submission_portion_size_fields ps
       |ON ps.food_id = f.id
       |WHERE m.survey_submission_id = {survey_submission_id}::uuid""".stripMargin

  val missingFoodsInsert =
    """INSERT INTO missing_foods VALUES(DEFAULT, {survey_id}, {user_id}, {name}, {brand}, {description}, {portion_size}, {leftovers}, {submitted_at})"""

  val missingFoodsSelect =
    """|SELECT survey_id, user_id, name, brand, description, portion_size, leftovers, submitted_at
       |FROM missing_foods
       |WHERE (submitted_at > {time_from} AND submitted_at < {time_to})""".stripMargin

  val popularityCountersSelect = """SELECT food_code, counter FROM popularity_counters WHERE food_code IN ({food_codes})"""

  val popularityCounterIncrement = """UPDATE popularity_counters SET counter = (counter + 1) WHERE food_code = {food_code}"""

  val popularityCounterInsert = """INSERT into popularity_counters VALUES ({food_code}, DEFAULT)"""

  val globalValueUpdate = """UPDATE global_values SET value = {value} WHERE name = {name}"""

  val globalValueInsert = """INSERT INTO global_values VALUES({name}, {value})"""

  val globalValueSelect = """SELECT value FROM global_values WHERE name = {name}"""

  val supportStaffSelectAll = """SELECT name, phone, email FROM support_staff"""

  val lastHelpRequestTimeUpdate = """UPDATE last_help_request_times SET last_help_request_time = {time} WHERE survey_id = {survey_id} AND user_id = {user_id}"""

  val lastHelpRequestTimeInsert = """INSERT INTO last_help_request_times VALUES ({survey_id}, {user_id}, {time})"""

  val lastHelpRequestTimeSelect = """SELECT last_help_request_time FROM last_help_request_times WHERE survey_id = {survey_id} AND user_id = {user_id}"""

}
