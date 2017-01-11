package uk.ac.ncl.openlab.intake24.services.foodindex.danish

import org.apache.commons.lang3.StringUtils
import org.workcraft.phrasesearch.{CaseInsensitiveString, WordOps}

class DanishWordOpsImpl extends WordOps {

  val stemmer = new DanishSnowballStemmer()
  val danishWords: Set[String] = scala.io.Source.fromInputStream(getClass.getResourceAsStream("da_dict.txt")).getLines().toSet

  def stem(word: CaseInsensitiveString): CaseInsensitiveString = {
    stemmer.setCurrent(word.lowerCase)
    stemmer.stem()
    val stemmed = stemmer.getCurrent
    CaseInsensitiveString(StringUtils.stripAccents(stemmed))
  }

  def splitCompound(word: CaseInsensitiveString): Seq[CaseInsensitiveString] = {
    def decompose(word: String, acc: List[String]): List[String] = {

      val candidates = Range(1, word.length).map(word.splitAt(_)).filter(c => danishWords.contains(c._2))

      // Performance: this needs to be lazy to avoid building the whole search space at once,
      // but since it is typically very small it doesn't matter

      candidates.map {
        case (l, r) => decompose(l, r :: acc)
      }.find(_.nonEmpty).getOrElse {
        if (danishWords.contains(word))
          word :: acc
        else
          Nil
      }
    }

    val decomposed = decompose(word.lowerCase, Nil)

    if (decomposed.isEmpty) // always return original word even if it is not a dictionary word
      List(word)
    else
      decomposed.map(CaseInsensitiveString(_))
  }
}