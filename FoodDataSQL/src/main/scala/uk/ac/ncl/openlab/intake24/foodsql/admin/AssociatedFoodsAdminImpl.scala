package uk.ac.ncl.openlab.intake24.foodsql.admin

import scala.Left
import scala.Right
import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.FoodHeader

import anorm.NamedParameter

import org.postgresql.util.PSQLException



import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError
import org.slf4j.LoggerFactory

import java.sql.Connection
import uk.ac.ncl.openlab.intake24.foodsql.SimpleValidation
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentCreateError
import anorm.SqlParser
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentRecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.foodsql.user.AssociatedFoodsUserImpl
import javax.sql.DataSource
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalDependentUpdateError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocaleOrParentError
import uk.ac.ncl.openlab.intake24.foodsql.modular.AssociatedFoodsAdminQueries

@Singleton
class AssociatedFoodsAdminStandaloneImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends AssociatedFoodsAdminImpl

trait AssociatedFoodsAdminImpl extends AssociatedFoodsAdminService with AssociatedFoodsUserImpl with AssociatedFoodsAdminQueries with SimpleValidation {

  private val logger = LoggerFactory.getLogger(classOf[AssociatedFoodsAdminImpl])

  def getAssociatedFoodsWithHeaders(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFoodWithHeader]] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- validateFoodAndLocale(foodCode, locale).right;
          result <- getAssociatedFoodsWithHeadersQuery(foodCode, locale).right
        ) yield result
      }
  }

  def createAssociatedFoods(assocFoods: Map[String, Seq[AssociatedFood]], locale: String): Either[LocaleOrParentError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        createAssociatedFoodsQuery(assocFoods, locale)
      }
  }

  def deleteAllAssociatedFoods(locale: String): Either[DatabaseError, Unit] = tryWithConnection {
    implicit conn =>
      deleteAllAssociatedFoodsQuery(locale)
  }

  def updateAssociatedFoods(foodCode: String, assocFoods: Seq[AssociatedFood], locale: String): Either[LocaleOrParentError, Unit] = tryWithConnection {
    implicit conn =>
      withTransaction {
        for (
          _ <- deleteAssociatedFoodsQuery(foodCode, locale).right;
          _ <- createAssociatedFoodsQuery(Map(foodCode -> assocFoods), locale).right
        ) yield ()
      }
  }
}
