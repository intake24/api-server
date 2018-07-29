package uk.ac.ncl.openlab.intake24.sql.tools.food

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.data.admin.{NewLocalFoodRecord, NewMainFoodRecord}
import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodsAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.foodindex.FoodIndexDataImpl
import uk.ac.ncl.openlab.intake24.sql.tools._

import scala.language.reflectiveCalls

object RestoreDeletedFoods extends App with DatabaseConnection with WarningMessage with ErrorHandler {

  val options = new ScallopConf(args) {
    val dbConfigDir = opt[String](required = true)

  }

  options.verify()

  val databaseConfigSource = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir(), DatabaseConfigChooser.default :+ ("Foods local backup", "backup.json"))
  val databaseConfigDest = DatabaseConfigChooser.chooseDatabaseConfiguration(options.dbConfigDir(), DatabaseConfigChooser.default :+ ("Foods local backup", "backup.json"))

  val dataSourceSource = getDataSource(databaseConfigSource)
  val dataSourceDest = getDataSource(databaseConfigDest)

  val foodsServiceSource = new FoodsAdminImpl(dataSourceSource)
  val foodsServiceDest = new FoodsAdminImpl(dataSourceDest)

  val foodsToCopy = Seq(
    "BECA",
    "CABP",
    "CBSW",
    "CHLA",
    "FFCS",
    "FFTS",
    "FRSF",
    "HAWP",
    "LBDF",
    "LFCB",
    "LFPS",
    "LFSB",
    "MACP",
    "MATT",
    "MCRC",
    "MCRT",
    "PACS",
    "PATS",
    "PEPP",
    "PSRS",
    "PSTB",
    "PSTM",
    "PSTR",
    "PSTT",
    "SBFO",
    "SEPA",
    "SLMI",
    "SPGS",
    "SPRM",
    "SSGE",
    "TUNP",
    "TUPB",
    "WWSM",
    //"FRFT"
  )

  // val missing = Vector("BECA", "BESW", "BNSW", "CABP", "CBSW", "CHLA", "CHOT", "CKSW", "CMNC", "CMNZ", "CTSM", "EMSW", "FFCS", "FFTS", "FRFT", "FRSF", "GBTS", "GF17", "GF31", "GFBC", "GFBG", "GFBM", "GFBR", "GFBS", "GFBT", "GFBW", "GFCB", "GFCC", "GFCH", "GFCI", "GFCK", "GFCM", "GFCN", "GFCO", "GFCR", "GFCS", "GFCT", "GFDI", "GFFB", "GFFL", "GFFP", "GFFS", "GFFT", "GFGB", "GFIP", "GFLM", "GFLN", "GFMA", "GFMC", "GFML", "GFMP", "GFMS", "GFNT", "GFOC", "GFPA", "GFPC", "GFPN", "GFPP", "GFPT", "GFRB", "GFSB", "GFSC", "GFSE", "GFSN", "GFVP", "GFWA", "GFWR", "GFWW", "GGSR", "HAWP", "HCSW", "HD_KEFR", "LBDF", "LFCB", "LFPS", "LFSB", "MACP", "MATT", "MCRC", "MCRT", "MMNZ", "NWZN", "NWZT", "NZAU", "NZCF", "NZDC", "NZFD", "NZFF", "NZHC", "NZLT", "NZMG", "NZMS", "NZND", "NZNO", "NZNV", "NZOF", "NZPA", "NZPM", "NZPN", "NZSM", "NZTF", "NZTL", "NZTM", "NZTN", "NZTP", "NZTR", "OJCB", "PACS", "PAPO", "PATS", "PBGF", "PEPP", "PEPS", "PESP", "PSAL", "PSRS", "PSTB", "PSTM", "PSTR", "PSTT", "SBFO", "SEPA", "SLMI", "SPGS", "SPRM", "SSGE", "TRNP", "TUNP", "TUPB", "WWSM")

  val locales = Seq("en_GB", "ar_AE", "en_NZ", "en_GB_gf", "pt_PT", "da_DK", "en_IN", "en_AU")


  foodsToCopy.foreach {
    code =>
      println(s"Copying $code...")

      val foodRecord = throwOnError(foodsServiceSource.getFoodRecord(code, "en_GB").map(_.main))

      val newRecord = NewMainFoodRecord(foodRecord.code, foodRecord.englishDescription, foodRecord.groupCode,
        foodRecord.attributes, foodRecord.parentCategories.map(_.code), foodRecord.localeRestrictions)

      println(s"\tcreating main food record...")

      throwOnError(foodsServiceDest.createFood(newRecord))
      locales.foreach {
        localeId =>

          val localRecord = throwOnError(foodsServiceSource.getFoodRecord(code, localeId).map(_.local))

          println(s"\tcreating local food record for ${localeId}...")

          val newLocalRecord = NewLocalFoodRecord(localRecord.localDescription, localRecord.doNotUse, localRecord.nutrientTableCodes,
            localRecord.portionSize, localRecord.associatedFoods.map(_.toAssociatedFood), localRecord.brandNames)

          throwOnError(foodsServiceDest.createLocalFoodRecords(Map(code -> newLocalRecord), localeId))
      }

      println()
  }

  println("Done!")
}
