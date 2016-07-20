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
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
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
import uk.ac.ncl.openlab.intake24.AsServedSet
import net.scran24.dbtool.SelectionDialog
import uk.ac.ncl.openlab.intake24.GuideImage
import java.awt.SystemColor
import java.awt.FlowLayout
import uk.ac.ncl.openlab.intake24.foodxml.StandardUnitDef
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter


class StandardPortionSizeEditor(params: Seq[PortionSizeMethodParameter], changesMade: () => Unit) extends PortionSizeEditor {
  case class GuideWrapper(guide: GuideImage) {
    override def toString = guide.description + " (" + guide.id + ")"
  }

  val undefinedIcon = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("net/scran24/admintool/warn.png")))
  undefinedIcon.setToolTipText("This parameter must be defined!")

  val initParam = params.map(p => (p.name, p.value)).toMap

  val unitTable = new StandardPortionUnitTable(StandardUnitDef.parsePortionSizeParameters(params), changesMade)

  val buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT))
  buttons.setOpaque(false)
  buttons.setPreferredSize(new Dimension(370, 40))

  val add = new JButton("Add unit")
  add.setFont(add.getFont.deriveFont(8))
  add.addActionListener(() => {
    unitTable.addUnit()
  })

  val delete = new JButton("Delete")
  delete.setFont(add.getFont.deriveFont(8))
  delete.addActionListener(() => {
    unitTable.delete()
  })
  
  val moveUp = new JButton("Move up")
  moveUp.setFont(add.getFont.deriveFont(8))
  moveUp.addActionListener(() => {
    unitTable.moveUp()
  })

  val moveDown = new JButton("Move down")
  moveDown.setFont(add.getFont.deriveFont(8))
  moveDown.addActionListener(() => {
    unitTable.moveDown()
  })
  

  buttons.add(moveUp)
  buttons.add(moveDown)
  buttons.add(add)
  buttons.add(delete)

  add(unitTable)
  add(new LineBreak)
  add(buttons)

  def parameters = StandardUnitDef.toPortionSizeParameters(unitTable.units)

  val methodName = "standard-portion"
}