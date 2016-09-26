package uk.ac.ncl.openlab.intake24.foodsql.foodindex

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError
import uk.ac.ncl.openlab.intake24.SplitList
import anorm.Macro
import anorm.SqlParser.str
import anorm._
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService


trait FoodIndexDataSharedImpl extends FoodDataSqlService {
  private case class SplitListRow(first_word: String, words: String)

  def splitList(locale: String): Either[DatabaseError, SplitList] = tryWithConnection {
    implicit conn =>
      val words = SQL("""SELECT words FROM split_words WHERE locale_id={locale}""")
        .on('locale -> locale)
        .executeQuery()
        .as(str("words").singleOpt).map {
          words => words.split("\\s+").toSeq
        }.getOrElse(Seq())

      val splitList = SQL("""SELECT first_word, words FROM split_list WHERE locale_id={locale}""")
        .on('locale -> locale)
        .executeQuery()
        .as(Macro.namedParser[SplitListRow].*)
        .map {
          case SplitListRow(first_word, words) => (first_word, words.split("\\s+").toSet)
        }.toMap

      Right(SplitList(words, splitList))
  }

  def synsets(locale: String): Either[DatabaseError, Seq[Set[String]]] = tryWithConnection {
    implicit conn =>
      Right(SQL("""SELECT synonyms FROM synonym_sets WHERE locale_id={locale}""")
        .on('locale -> locale)
        .executeQuery()
        .as(str("synonyms").*)
        .map(row => row.split("\\s+").toSet))
  }
}