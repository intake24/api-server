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

This file is based on Intake24 v1.0.

© Crown copyright, 2012, 2013, 2014

Licensed under the Open Government Licence 3.0: 

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool.portion

import javax.swing.JPanel
import net.scran24.fooddef.PortionSizeMethod
import org.workcraft.gui.SimpleFlowLayout
import javax.swing.JLabel
import javax.swing.JButton
import org.workcraft.gui.SimpleFlowLayout.LineBreak
import java.awt.Dimension
import java.awt.Color
import javax.swing.ImageIcon
import javax.swing.JCheckBox
import net.scran24.dbtool.SwingUtil._
import net.scran24.dbtool.Util._
import net.scran24.fooddef.AsServedSet
import net.scran24.dbtool.SelectionDialog
import net.scran24.fooddef.GuideImage
import java.awt.SystemColor
import javax.swing.JComboBox
import java.awt.event.ItemListener
import java.awt.event.ItemEvent
import net.scran24.fooddef.PortionSizeMethodParameter

class MilkOnCerealPortionSizeEditor(params: Seq[PortionSizeMethodParameter], changesMade: () => Unit) extends PortionSizeEditor {
  val label = new JLabel("This method has no parameters")

  add(label)
  
  def parameters = Seq()

  val methodName = "milk-on-cereal"
}