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
import uk.ac.ncl.openlab.intake24.NutrientTable
import uk.ac.ncl.openlab.intake24.services.nutrition.NutrientDataManagementService
import uk.ac.ncl.openlab.intake24.NutrientTableRecord
import anorm.NamedParameter
import anorm.BatchSql
import java.sql.BatchUpdateException
import uk.ac.ncl.openlab.intake24.NutrientType

class NutrientDataManagementSqlImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends NutrientDataManagementService with SqlDataService {

  def nutrientTables(): Seq[NutrientTable] = tryWithConnection {
    implicit conn =>

      var query = """SELECT id, description FROM nutrient_tables ORDER BY description"""

      SQL(query).executeQuery().as(Macro.namedParser[NutrientTable].*)
  }

  def nutrientTable(id: String): Option[NutrientTable] = tryWithConnection {
    implicit conn =>
      var query = """SELECT id, description FROM nutrient_tables WHERE id = {id} ORDER BY english_name"""

      SQL(query).on('id -> id).executeQuery().as(Macro.namedParser[NutrientTable].singleOpt)
  }

  def createNutrientTable(data: NutrientTable) = tryWithConnection {
    implicit conn =>
      var query = """INSERT INTO nutrient_tables VALUES({id}, {description})"""

      SQL(query).on('id -> data.id, 'description -> data.description).execute()

  }

  def updateNutrientTable(id: String, data: NutrientTable) = tryWithConnection {
    implicit conn =>
      var query = """UPDATE nutrient_tables SET id={new_id}, description={description} WHERE id = {id}"""

      SQL(query).on('id -> id, 'new_id -> data.id, 'description -> data.description).execute()
  }

  def deleteNutrientTable(id: String) = tryWithConnection {
    implicit conn =>
      val query = """DELETE FROM nutrient_tables WHERE id={id}"""

      SQL(query).on('id -> id).execute()
  }

  def createNutrientTableRecords(records: Seq[NutrientTableRecord]) = tryWithConnection {
    implicit conn =>
      val query = """INSERT INTO nutrient_table_records VALUES({code},{nutrient_table_id},{nutrient_type_id},{units_per_100g})"""

      val params =
        records.map(r => Seq[NamedParameter]('code -> r.tableCode, 'nutrient_table_id -> r.table_id, 'nutrient_type_id -> r.nutrient_id, 'units_per_100g -> r.unitsPer100g))

      try {
        BatchSql(query, params).execute()
      } catch {
        case e: BatchUpdateException => throw new RuntimeException(e.getMessage, e.getNextException)
      }
  }

  /*def createNutrientTypes(records: Seq[NutrientType]) = tryWithConnection {
    implicit conn =>
      val query = """INSERT INTO nutrient_types VALUES({code},{nutrient_table_id},{nutrient_type_id},{nutrient_unit_id},{units_per_100g})"""

      val params =
        records.map(r => Seq[NamedParameter]('id -> r.id, 'description -> r.description))

      try {
        BatchSql(query, params).execute()
      } catch {
        case e: BatchUpdateException => throw new RuntimeException(e.getMessage, e.getNextException)
      }

  }*/

}
