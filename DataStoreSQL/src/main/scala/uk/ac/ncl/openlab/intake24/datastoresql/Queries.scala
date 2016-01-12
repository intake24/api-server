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

  val usersInsert = """INSERT INTO users VALUES ({id}, {survey_id}, {password_hash}, {password_salt}, {password_hasher})"""

  val usersSelectBySurvey = """SELECT id as user_id, survey_id, password_hash, password_salt, password_hasher FROM users WHERE survey_id = {survey_id} ORDER BY (survey_id, id)"""

  val usersSelectByRole =
    """|SELECT users.survey_id as survey_id, users.id as user_id, password_hash, password_salt, password_hasher FROM users 
       |JOIN user_roles 
       |ON (users.survey_id = user_roles.survey_id AND users.id = user_roles.user_id) 
       |WHERE (role = {role} AND users.survey_id = {survey_id})
       |ORDER BY (users.survey_id, users.id)""".stripMargin

  val userSelect = """SELECT password_hash, password_salt, password_hasher FROM users WHERE (survey_id={survey_id} AND id={user_id})"""

  val usersDeleteByRole = """DELETE FROM users WHERE id IN (SELECT user_id FROM user_roles WHERE role = {role}) AND survey_id = {survey_id}"""

  val userRolesInsert = """INSERT INTO user_roles VALUES (DEFAULT, {survey_id}, {user_id}, {role})"""

  val userRolesSelectByUser = """SELECT role FROM user_roles WHERE (survey_id={survey_id} AND user_id={user_id})"""

  val userRolesSelectBySurvey = """SELECT survey_id, user_id, role FROM user_roles WHERE survey_id = {survey_id} ORDER BY (survey_id, user_id)"""

  val userRolesSelectByRole =
    """|SELECT r2.survey_id, r2.user_id, r2.role FROM user_roles r1
       |JOIN user_roles r2
       |ON  (r1.survey_id = r2.survey_id AND r1.user_id = r2.user_id)
       |WHERE (r1.role = {role} AND r1.survey_id = {survey_id})
       |ORDER BY (r2.survey_id, r2.user_id)""".stripMargin

  val userPermissionsInsert = """INSERT INTO user_permissions VALUES (DEFAULT, {survey_id}, {user_id}, {permission})"""

  val userPermissionsSelectByUser = """SELECT permission FROM user_permissions WHERE (survey_id={survey_id} AND user_id={user_id})"""

  val userPermissionsSelectBySurvey = """SELECT survey_id, user_id, permission FROM user_permissions WHERE survey_id = {survey_id} ORDER BY (survey_id, user_id)"""

  val userPermissionsSelectByRole =
    """|SELECT 
       |r.survey_id as survey_id, r.user_id as user_id, permission 
       |FROM user_roles r
       |JOIN user_permissions p
       |ON (r.survey_id = p.survey_id AND r.user_id = p.user_id)
       |WHERE (role = {role} AND r.survey_id = {survey_id})
       |ORDER BY (r.survey_id, r.user_id)""".stripMargin

  val userCustomFieldsInsert = """INSERT INTO user_custom_fields VALUES (DEFAULT, {survey_id}, {user_id}, {name}, {value})"""

  val userCustomFieldsSelectByUser = """SELECT name, value FROM user_custom_fields WHERE (user_id={user_id} AND survey_id={survey_id})"""

  val userCustomFieldsSelectBySurvey = """SELECT survey_id, user_id, name, value FROM user_custom_fields WHERE survey_id = {survey_id} ORDER BY (survey_id, user_id)"""

  val userCustomFieldsSelectByRole =
    """|SELECT r.survey_id as survey_id, r.user_id as user_id, name, value 
       |FROM user_roles r JOIN user_custom_fields f
       |ON (r.survey_id = f.survey_id AND r.user_id = f.user_id)
       |WHERE (role = 'staff' AND r.survey_id = 'demo')
       |ORDER BY (r.survey_id, r.user_id)""".stripMargin

  val userCustomFieldsDelete = """DELETE from user_custom_fields WHERE survey_id = {survey_id} AND user_id = {user_id}"""

  val surveysInsert = """INSERT INTO surveys VALUES ({id}, {state}, {start_date}, {end_date}, {scheme_id}, {locale}, {allow_gen_users}, {suspension_reason}, {survey_monkey_url})"""

  val surveysInsertInit = """INSERT INTO surveys VALUES ({id}, 0, DEFAULT, DEFAULT, {scheme_id}, {locale}, {allow_gen_users}, '', {survey_monkey_url})"""

  val surveysSelect = """SELECT state, start_date, end_date, scheme_id, locale, allow_gen_users, suspension_reason, survey_monkey_url FROM surveys WHERE id = {survey_id}"""

  val surveysUpdate = """UPDATE surveys SET state={state}, start_date={start_date}, end_date={end_date}, scheme_id={scheme_id}, locale={locale}, allow_gen_users={allow_gen_users}, suspension_reason={suspension_reason}, survey_monkey_url={survey_monkey_url} WHERE id = {survey_id}"""

  val surveySubmissionsInsert = """INSERT INTO survey_submissions VALUES ({id}::uuid, {survey_id}, {user_id}, {start_time}, {end_time}, {log})"""

  val surveyCustomFieldsInsert = """INSERT INTO survey_submission_custom_fields VALUES (DEFAULT, {survey_submission_id}::uuid, {name}, {value})"""

  val surveyUserCustomFieldsInsert = """INSERT INTO survey_submission_user_custom_fields VALUES (DEFAULT, {survey_submission_id}::uuid, {name}, {value})"""

  val surveyMealsInsert = """INSERT INTO survey_submission_meals VALUES (DEFAULT, {survey_submission_id}::uuid, {hours}, {minutes}, {name})"""

  val surveyMealsCustomFieldsInsert = """INSERT INTO survey_submission_meal_custom_fields VALUES (DEFAULT, {meal_id}, {name}, {value})"""

  val surveyFoodsInsert = """INSERT INTO survey_submission_foods VALUES (DEFAULT, {meal_id}, {code}, {english_description}, {local_description}, {ready_meal}, {search_term}, {portion_size_method_id}, {reasonable_amount},{food_group_id},{food_group_english_description},{food_group_local_description},{brand},{nutrient_table_id},{nutrient_table_code})"""

  val surveyFoodCustomFieldsInsert = """INSERT INTO survey_submission_food_custom_fields VALUES (DEFAULT, {food_id}, {name}, {value})"""

  val surveyFoodPortionSizeFieldsInsert = """INSERT INTO survey_submission_portion_size_fields VALUES (DEFAULT, {food_id}, {name}, {value})"""

  val surveyFoodNutrientValuesInsert = """INSERT INTO survey_submission_nutrients VALUES (DEFAULT, {food_id}, {name}, {value})"""

  val surveySubmissionsSelectByTime = """SELECT id, survey_id, user_id, start_time, end_time, log FROM survey_submissions WHERE start_time > {time_from} AND end_time < {time_to}"""

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
    """|SELECT f.id as food_id, n.name as n_name, n.amount as n_amount
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

  def batchUserInsert(survey_id: String, userRecords: Seq[SecureUserRecord])(implicit conn: Connection) = {
    val userParams = userRecords.map {
      userRecord =>
        Seq[NamedParameter]('id -> userRecord.username, 'survey_id -> survey_id, 'password_hash -> userRecord.passwordHashBase64, 'password_salt -> userRecord.passwordSaltBase64,
          'password_hasher -> userRecord.passwordHasher)
    }

    if (!userParams.isEmpty)
      try {
        BatchSql(Queries.usersInsert, userParams).execute()

        val roleParams = userRecords.flatMap {
          userRecord =>
            userRecord.roles.map(role => Seq[NamedParameter]('survey_id -> survey_id, 'user_id -> userRecord.username, 'role -> role))
        }

        if (!roleParams.isEmpty)
          BatchSql(Queries.userRolesInsert, roleParams).execute()

        val permissionParams = userRecords.flatMap {
          userRecord => userRecord.permissions.map(permission => Seq[NamedParameter]('survey_id -> survey_id, 'user_id -> userRecord.username, 'permission -> permission))
        }

        if (!permissionParams.isEmpty)
          BatchSql(Queries.userPermissionsInsert, permissionParams).execute()

        val userCustomFieldParams = userRecords.flatMap {
          userRecord =>
            userRecord.customFields.map {
              case (name, value) => Seq[NamedParameter]('survey_id -> survey_id, 'user_id -> userRecord.username, 'name -> name, 'value -> value)
            }
        }

        if (!userCustomFieldParams.isEmpty)
          BatchSql(Queries.userCustomFieldsInsert, userCustomFieldParams).execute()

      } catch {
        case e: java.sql.BatchUpdateException => throw e.getNextException
      }
  }
}
