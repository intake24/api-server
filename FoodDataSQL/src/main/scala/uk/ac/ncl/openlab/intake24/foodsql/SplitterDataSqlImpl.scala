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

import net.scran24.fooddef.SplitList
import anorm._
import anorm.SqlParser._

trait SplitterDataSqlImpl extends SqlDataService {

  private case class SplitListRow(first_word: String, words: String)

  def splitList(locale: String): SplitList = tryWithConnection {
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

      SplitList(words, splitList)
  }
}
