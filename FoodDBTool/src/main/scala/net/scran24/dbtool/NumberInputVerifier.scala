/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import javax.swing.InputVerifier
import javax.swing.JComponent
import javax.swing.JFormattedTextField
import java.text.ParseException
import javax.swing.JTextField
import javax.swing.border.Border

class NumberInputVerifier (val okBorder: Border, val errorBorder: Border) extends InputVerifier {
  def verify(input: JComponent): Boolean = {
    val ftf = input.asInstanceOf[JTextField]

    try {
      ftf.getText().toInt
      ftf.setBorder(okBorder)
      true
    } catch {
      case e: NumberFormatException => {
        ftf.setBorder(errorBorder)
        false
      }
    }
  }

  override def shouldYieldFocus(input: JComponent): Boolean = verify(input)
}