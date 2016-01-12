/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import javax.swing.JPanel
import org.workcraft.gui.SimpleFlowLayout
import javax.swing.BorderFactory

abstract class DefEditor extends JPanel {
  setLayout(new SimpleFlowLayout(400))
  setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
  
  var changed = false
  
  def acceptChanges(): Boolean
  def description: String
}