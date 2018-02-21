package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import org.rogach.scallop.ScallopConf
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.api.data.admin.NewLocalCategoryRecord
import uk.ac.ncl.openlab.intake24.foodsql.admin.{CategoriesAdminImpl, FoodsAdminImpl}
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, DatabaseConnection, ErrorHandler, WarningMessage}

object AustralianCategories extends App with WarningMessage with ErrorHandler with DatabaseConnection {

  private val logger = LoggerFactory.getLogger(getClass)

  val options = new ScallopConf(args) with DatabaseConfigurationOptions

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  val locale = "en_AU"

  displayWarningMessage(s"This will update $locale in ${dbConfig.host}/${dbConfig.database}. Are you sure?")

  val dataSource = getDataSource(dbConfig)

  val service = new CategoriesAdminImpl(dataSource)

  val index = new FoodIndexDataImpl(dataSource)

  val allCategories = throwOnError(index.indexableCategories("en_GB")).map(_.code)

  val total = allCategories.size

  var processed = 1

  allCategories.foreach {
    code =>
      logger.info(s"Processing $code ($processed of $total)")

      processed += 1

      throwOnError(service.getCategoryRecord(code, locale).flatMap {
        r =>
          r.local.version match {
            case Some(version) =>
              service.updateLocalCategoryRecord(code, r.local.toUpdate.copy(localDescription = Some(r.main.englishDescription)), locale)
            case None =>
              service.createLocalCategoryRecord(code, NewLocalCategoryRecord(Some(r.main.englishDescription), Seq()), locale)
          }
      })

  }

  logger.info(s"Done!")
}
