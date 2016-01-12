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

package org.workcraft.phrasesearch

case class Metaphone3Encoder() extends PhoneticEncoder {
  private val m3 = new Metaphone3()
  
  m3.SetKeyLength(5)
  m3.SetEncodeVowels(true)
  m3.SetEncodeExact(true)
  
  def encode(word: CaseInsensitiveString): Set[CaseInsensitiveString] = {
    m3.SetWord(word.lowerCase)
    m3.Encode()

    val meta1 = m3.GetMetaph()
    val meta2 = m3.GetAlternateMetaph()

    if (meta2.isEmpty()) Set(CaseInsensitiveString(meta1)) else Set(CaseInsensitiveString(meta1), CaseInsensitiveString(meta2))
  }
}