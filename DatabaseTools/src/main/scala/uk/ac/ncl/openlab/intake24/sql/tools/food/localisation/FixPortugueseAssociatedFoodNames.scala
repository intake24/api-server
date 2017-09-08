package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation

import anorm.{BatchSql, NamedParameter}
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.sql.tools.{DatabaseConfigurationOptions, DatabaseConnection, ErrorHandler, WarningMessage}

object FixPortugueseAssociatedFoodNames extends App with WarningMessage with DatabaseConnection with ErrorHandler {

  val translations = Seq(
    "mixer" -> "bebida",
    "milk" -> "leite",
    "sugar" -> "açúcar",
    "mayonnaise" -> "maionese",
    "sauce" -> "molho",
    "butter or margarine" -> "manteiga ou margarina",
    "jam/preserve" -> "compota",
    "other spreads" -> "creme de barrar",
    "ice cream" -> "gelado",
    "cream" -> "chantilly",
    "rice" -> "arroz",
    "fries" -> "batatas fritas",
    "drink" -> "bebida",
    "bread or toast" -> "pão ou torrada",
    "gravy" -> "molho",
    "jam/spread" -> "compota/creme de barrar",
    "bread or toast" -> "pão ou torrada",
    "bread" -> "pão",
    "toast" -> "torrada",
    "sugar/sauce" -> "açúcar/molho",
    "crackers or crispbreads" -> "biscoitos ou batatas fritas",
    "noodles or rice" -> "macarrão ou arroz",
    "salsa dip" -> "mergulho de salsa",
    "other dips" -> "outros mergulhos",
    "burger" -> "pão",
    "custard" -> "creme custard",
    "quindim" -> "creme custard",
    "pineapple" -> "ananás",
    "abacaxi" -> "ananás",
    "egg" -> "ovo",
    "butter/margarine" -> "manteiga ou margarina"

  )

  trait Options extends DatabaseConfigurationOptions {

  }

  val options = new ScallopConf(args) with Options

  options.verify()

  val dbConfig = chooseDatabaseConfiguration(options)

  displayWarningMessage(s"This will update Portuguese associated foods in ${dbConfig.host}/${dbConfig.database}. Are you sure?")

  val dataSource = getDataSource(dbConfig)

  val params = translations.map {
    case (en, pt) =>
      Seq[NamedParameter]('en_name -> en, 'pt_name -> pt, 'locale_id -> "pt_PT")
  }

  implicit val con = dataSource.getConnection()

  BatchSql("UPDATE associated_foods SET generic_name={pt_name} WHERE generic_name={en_name} AND locale_id={locale_id}", params.head, params.tail: _*).execute()

  con.close()
}
