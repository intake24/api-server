package uk.ac.ncl.openlab.intake24.foodsql.admin

import javax.sql.DataSource

import com.google.inject.name.Named
import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.data.AssociatedFood
import uk.ac.ncl.openlab.intake24.api.data.admin.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.errors.{LocalLookupError, LocaleOrParentError, UnexpectedDatabaseError}
import uk.ac.ncl.openlab.intake24.foodsql.SimpleValidation
import uk.ac.ncl.openlab.intake24.foodsql.modular.AssociatedFoodsAdminQueries
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.user.AssociatedFoodsService
import uk.ac.ncl.openlab.intake24.sql.SqlDataService

@Singleton
class AssociatedFoodsAdminImpl @Inject()(@Named("intake24_foods") val dataSource: DataSource, associatedFoodsService: AssociatedFoodsService) extends AssociatedFoodsAdminService with AssociatedFoodsAdminQueries with SimpleValidation with SqlDataService {

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

  def deleteAllAssociatedFoods(locale: String): Either[UnexpectedDatabaseError, Unit] = tryWithConnection {
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

  def getAssociatedFoods(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFood]] =
    associatedFoodsService.getAssociatedFoods(foodCode, locale)
}
