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

package uk.ac.ncl.openlab.intake24.foodsql

import com.google.inject.Inject
import com.google.inject.name.Named

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import javax.sql.DataSource
import net.scran24.fooddef.NutrientTable
import uk.ac.ncl.openlab.intake24.services.NutrientTableManagementService

class NutrientTableManagementSqlImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends NutrientTableManagementService with SqlDataService {

  def list: Seq[NutrientTable] = tryWithConnection {
    implicit conn =>

      var query = """SELECT id, description FROM nutrient_tables ORDER BY description"""

      SQL(query).executeQuery().as(Macro.namedParser[NutrientTable].*)
  }

  def get(id: String): Option[NutrientTable] = tryWithConnection {
    implicit conn =>
      var query = """SELECT id, description FROM nutrient_tables WHERE id = {id} ORDER BY english_name"""

      SQL(query).on('id -> id).executeQuery().as(Macro.namedParser[NutrientTable].singleOpt)
  }

  def create(data: NutrientTable) = tryWithConnection {
    implicit conn =>
      var query = """INSERT INTO nutrient_tables VALUES({id}, {description})"""

      SQL(query).on('id -> data.id, 'description -> data.description).execute()

  }

  def update(id: String, data: NutrientTable) = tryWithConnection {
    implicit conn =>
      var query = """UPDATE nutrient_tables SET id={new_id}, description={description} WHERE id = {id}"""

      SQL(query).on('id -> id, 'new_id -> data.id, 'description -> data.description).execute()
  }

  def delete(id: String) = tryWithConnection {
    implicit conn =>
      val query = """DELETE FROM nutrient_tables WHERE id={id}"""
      
      SQL(query).on('id -> id).execute()
  }
}
