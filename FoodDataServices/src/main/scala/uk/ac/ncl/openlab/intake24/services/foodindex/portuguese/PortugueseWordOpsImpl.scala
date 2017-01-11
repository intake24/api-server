package uk.ac.ncl.openlab.intake24.services.foodindex.portuguese

import org.apache.commons.lang3.StringUtils
import org.workcraft.phrasesearch.{CaseInsensitiveString, WordOps}
import ptstemmer.implementations.SavoyStemmer

class PortugueseWordOpsImpl extends WordOps {
  val stemmer = new SavoyStemmer()

  def stem(word: CaseInsensitiveString): CaseInsensitiveString = CaseInsensitiveString(StringUtils.stripAccents(stemmer.getWordStem(word.lowerCase)))

  def splitCompound(word: CaseInsensitiveString): Seq[CaseInsensitiveString] = Seq(word)
}