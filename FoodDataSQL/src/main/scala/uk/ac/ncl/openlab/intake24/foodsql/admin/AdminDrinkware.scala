package uk.ac.ncl.openlab.intake24.foodsql.admin

import uk.ac.ncl.openlab.intake24.DrinkwareHeader
import anorm.SQL
import anorm.Macro
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.foodsql.UserDrinkware

trait AdminDrinkware extends UserDrinkware {

  def allDrinkware(): Seq[DrinkwareHeader] = tryWithConnection {
    implicit conn =>
      SQL("""SELECT id, description FROM drinkware_sets ORDER BY description ASC""").executeQuery().as(Macro.namedParser[DrinkwareHeader].*)
  }
}
