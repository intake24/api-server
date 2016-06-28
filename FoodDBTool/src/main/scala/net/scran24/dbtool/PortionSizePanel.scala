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

package net.scran24.dbtool

import javax.swing.JPanel
import org.workcraft.gui.SimpleFlowLayout
import javax.swing.JOptionPane
import net.scran24.fooddef.PortionSizeMethod
import javax.swing.JLabel
import java.awt.Color
import java.awt.Font
import org.workcraft.gui.SimpleFlowLayout.LineBreak
import SwingUtil._
import javax.swing.JButton

class PortionSizePanel(entryCode: String, food: Boolean, initial: Seq[PortionSizeMethod], resources: PortionResources, imageDirectory: ImageDirectory, resolver: PortionSizeResolver, changesMade: () => Unit) extends JPanel {
  setLayout(new SimpleFlowLayout(400))

  val inheritedPortionSizeContainer = new JPanel(new SimpleFlowLayout(400))
  val portionSizeContainer = new JPanel(new SimpleFlowLayout(400))
  val portionLabel = new JLabel("Portion size estimation")
  portionLabel.setFont(new Font("Dialog", 0, 20))

  var portionSizes: Seq[EditablePortionSizePanel] = Seq()

  def deletePortionSize(panel: EditablePortionSizePanel) = {
    if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this portion size estimation method?", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
      portionSizeContainer.remove(panel)
      portionSizes = portionSizes.filterNot(_ == panel)
      if (portionSizes.isEmpty) showInheritedPortionSizes()
      handleNoPortionSize()
      changesMade()
      revalidate()
      repaint()
    }
  }

  def addPortionSize(method: PortionSizeMethod, init: Boolean) = {
    if (portionSizes.isEmpty) {
      portionSizeContainer.removeAll()
      hideInheritedPortionSizes()
    }
    val panel = new EditablePortionSizePanel(method, resources, imageDirectory, deletePortionSize, changesMade)
    portionSizeContainer.add(panel)
    portionSizes :+= panel
    if (!init) changesMade()
    revalidate()
    repaint()
  }

  def handleNoPortionSize() =
    if (portionSizes.isEmpty && inheritedPortionSizeContainer.getComponentCount() == 0) {
      val message = new JLabel("<html>Neither this food/category nor any of its parent categories<br/> have a portion size esitmation method defined.</html>")
      portionSizeContainer.add(message)
      revalidate()
      repaint()
    }

  def hideInheritedPortionSizes() = {
    portionLabel.setText("Portion size estimation")
    inheritedPortionSizeContainer.removeAll()
    revalidate()
    repaint()
  }

  def showInheritedPortionSizes() = {
    val inherited = if (food)
      resolver.foodInheritedPortionSize(entryCode)
    else
      resolver.categoryInheritedPortionSize(entryCode)

    if (!inherited.isEmpty) {
      portionLabel.setText("Portion size estimation (inherited)")
      inheritedPortionSizeContainer.removeAll()
      inherited.foreach(method => {
        val panel = new EditablePortionSizePanel(method, resources, imageDirectory, _ => {}, () => {})
        recursiveSetEnabled(panel, false)
        inheritedPortionSizeContainer.add(panel)
      })
      revalidate()
      repaint()
    }
  }

  def addInitialPortionSizes() = {
    if (initial.isEmpty) {
      showInheritedPortionSizes()
    } else initial.foreach(addPortionSize(_, true))
    handleNoPortionSize()
  }

  def discard() = {
    portionSizeContainer.removeAll()
    portionSizes = Seq()
    addInitialPortionSizes()
  }

  add(portionLabel)
  add(new LineBreak)

  add(inheritedPortionSizeContainer)
  add(portionSizeContainer)
  add(new LineBreak)
  addInitialPortionSizes()

  val addPortionSizeButton = new JButton("Add a portion size method")
  addPortionSizeButton.addActionListener(() => {
    addPortionSize(new PortionSizeMethod("as-served", "use_an_image", "portion/placeholder.jpg", false, Seq()), false)
  })

  add(addPortionSizeButton)
}