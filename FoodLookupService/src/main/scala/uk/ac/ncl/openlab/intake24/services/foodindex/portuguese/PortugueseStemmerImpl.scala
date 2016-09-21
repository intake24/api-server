package uk.ac.ncl.openlab.intake24.services.foodindex.portuguese

import org.workcraft.phrasesearch.WordStemmer
import org.workcraft.phrasesearch.CaseInsensitiveString

import ptstemmer.implementations.OrengoStemmer
import org.apache.commons.lang3.StringUtils
import ptstemmer.implementations.PorterStemmer

class PortugueseStemmerImpl extends WordStemmer {
  val stemmer = new PorterStemmer()
  
    def stem(word: CaseInsensitiveString): CaseInsensitiveString = {
    CaseInsensitiveString(StringUtils.stripAccents(stemmer.getWordStem(word.lowerCase)))
  }
}