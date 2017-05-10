package uk.ac.ncl.openlab.intake24.services.foodindex.arabic

import org.workcraft.phrasesearch.{CaseInsensitiveString, WordOps}

class ArabicWordOpsImpl extends WordOps {

  def stem(word: CaseInsensitiveString): CaseInsensitiveString = word

  def splitCompound(word: CaseInsensitiveString): Seq[CaseInsensitiveString] = Seq(word)
}