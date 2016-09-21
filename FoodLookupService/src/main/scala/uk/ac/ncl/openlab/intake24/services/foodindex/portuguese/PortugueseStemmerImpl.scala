package uk.ac.ncl.openlab.intake24.services.foodindex.portuguese

import org.workcraft.phrasesearch.WordStemmer
import org.workcraft.phrasesearch.CaseInsensitiveString

import ptstemmer.implementations.OrengoStemmer;
import org.apache.commons.lang3.StringUtils

class PortugueseStemmerImpl extends WordStemmer {
  val stemmer = new OrengoStemmer();

  def stem(word: CaseInsensitiveString): CaseInsensitiveString = {
    CaseInsensitiveString(StringUtils.stripAccents(stemmer.getWordStem(word.lowerCase)))
  }
}