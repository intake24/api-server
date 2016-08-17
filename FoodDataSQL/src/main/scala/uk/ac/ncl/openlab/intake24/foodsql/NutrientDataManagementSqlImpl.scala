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
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError

class NutrientDataManagementSqlImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends NutrientDataManagementService with SqlDataService {

  def allNutrientTables(): Either[DatabaseError, Seq[NutrientTable]] = tryWithConnection {
    implicit conn =>

      var query = """SELECT id, description FROM nutrient_tables ORDER BY description"""

      Right(SQL(query).executeQuery().as(Macro.namedParser[NutrientTable].*))
  }

  def nutrientTable(id: String): Either[LookupError, NutrientTable] = tryWithConnection {
    implicit conn =>
      var query = """SELECT id, description FROM nutrient_tables WHERE id = {id} ORDER BY english_name"""

      SQL(query).on('id -> id).executeQuery().as(Macro.namedParser[NutrientTable].singleOpt) match {
        case Some(table) => Right(table)
        case None => Left(RecordNotFound)
      }
  }

  def createNutrientTable(data: NutrientTable): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      var query = """INSERT INTO nutrient_tables VALUES({id}, {description})"""

      SQL(query).on('id -> data.id, 'description -> data.description).execute()

      Right(())
  }

  def updateNutrientTable(id: String, data: NutrientTable): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      var query = """UPDATE nutrient_tables SET id={new_id}, description={description} WHERE id = {id}"""

      val affectedRows = SQL(query).on('id -> id, 'new_id -> data.id, 'description -> data.description).executeUpdate()

      if (affectedRows == 0)
        Left(RecordNotFound)
      else
        Right(())
  }

  def deleteNutrientTable(id: String): Either[LookupError, Unit] = tryWithConnection {
    implicit conn =>
      val query = """DELETE FROM nutrient_tables WHERE id={id}"""

      val affectedRows = SQL(query).on('id -> id).executeUpdate()

      if (affectedRows == 0)
        Left(RecordNotFound)
      else
        Right(())
  }

  def createNutrientTableRecords(records: Seq[NutrientTableRecord]): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>

      conn.setAutoCommit(false)

      val recordQuery = """INSERT INTO nutrient_table_records VALUES({id},{nutrient_table_id})"""

      val recordParams =
        records.map(r => Seq[NamedParameter]('id -> r.record_id, 'nutrient_table_id -> r.table_id))

      val nutrientsQuery = """INSERT INTO nutrient_table_records_nutrients VALUES({record_id},{nutrient_table_id},{nutrient_type_id},{units_per_100g})"""

      val nutrientParams =
        records.flatMap {
          record =>
            record.nutrients.map {
              case (nutrientType, unitsPer100g) =>
                Seq[NamedParameter]('record_id -> record.record_id, 'nutrient_table_id -> record.table_id, 'nutrient_type_id -> nutrientType.id, 'units_per_100g -> unitsPer100g)
            }
        }

      BatchSql(recordQuery, recordParams).execute()
      BatchSql(nutrientsQuery, nutrientParams).execute()

      conn.commit()
      
      Right(())
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
