package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import java.io.{File, FileReader}

import com.opencsv.CSVReader
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.data.AssociatedFood
import uk.ac.ncl.openlab.intake24.foodsql.admin.AssociatedFoodsAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.user.AssociatedFoodsServiceImpl
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, DatabaseConnection, ErrorHandler, WarningMessage}

import scala.collection.JavaConverters._

object DanishAssociatedFoodsImport extends App with WarningMessage with DatabaseConnection with ErrorHandler {

  val locale = "da_DK"

  trait Options extends DatabaseConfigurationOptions {

    val csvPath = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  displayWarningMessage(s"This will update Danish associated foods in ${dbConfig.host}/${dbConfig.database}. Are you sure?")

  val dataSource = getDataSource(dbConfig)

  val associatedFoodsUserService = new AssociatedFoodsServiceImpl(dataSource)
  val associatedFoodsService = new AssociatedFoodsAdminImpl(dataSource, associatedFoodsUserService)

  throwOnError(associatedFoodsService.deleteAllAssociatedFoods(locale))


  val reader = new CSVReader(new FileReader(new File(options.csvPath())))

  val associatedFoodsMap = reader.readAll().asScala
    .drop(1)
    .filterNot(row => row(7).trim.isEmpty || row(9).trim.isEmpty)
    .groupBy(_ (0)).map {
    case (foodCode, rows) =>

      val enGbFoods = throwOnError(associatedFoodsService.getAssociatedFoods(foodCode, "en_GB"))

      val assocFoods = rows.map {
        row =>
          val assocFoodCode = row(1).trim
          val assocCategoryCode = row(2).trim

          if (assocFoodCode.isEmpty && assocCategoryCode.isEmpty)
            throw new RuntimeException(s"Both food and category codes are empty for $foodCode")

          val assocFoodOrCategoryCode = if (assocFoodCode.nonEmpty) Left(assocFoodCode) else Right(assocCategoryCode)

          val linkAsMain = enGbFoods.find(ef => ef.foodOrCategoryCode == assocFoodOrCategoryCode) match {
            case Some(f) => f.linkAsMain
            case None => throw new RuntimeException(s"Cannot find enGB assoc food record for $foodCode -> $assocFoodOrCategoryCode")
          }


          AssociatedFood(assocFoodOrCategoryCode, row(7), linkAsMain, row(9))

      }

      (foodCode, assocFoods.toSeq)
  }

  throwOnError(associatedFoodsService.createAssociatedFoods(associatedFoodsMap, locale))
}
