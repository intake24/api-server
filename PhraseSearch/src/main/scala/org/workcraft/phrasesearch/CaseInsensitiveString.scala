/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package org.workcraft.phrasesearch

import scala.language.implicitConversions

class CaseInsensitiveString(original: String) extends Proxy with Ordered[CaseInsensitiveString] {
  val lowerCase: String = original.toLowerCase

  def self = lowerCase

  def compare(other: CaseInsensitiveString) = lowerCase.compareTo(other.lowerCase)

  override def toString = lowerCase

  def isEmpty = lowerCase.isEmpty
}

object CaseInsensitiveString {
  implicit def apply(s: String) = new CaseInsensitiveString(s)
}