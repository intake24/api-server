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
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import javax.swing.JSlider
import javax.swing.event.ChangeListener
import javax.swing.event.ChangeEvent
import java.awt.SystemColor
import java.awt.event.ItemListener
import java.awt.event.ItemEvent
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter

class DrinkScalePortionSizeEditor(params: Seq[PortionSizeMethodParameter], drinkwareDefs: Seq[DrinkwareSet], changesMade: () => Unit) extends PortionSizeEditor {
  case class DrinkwareWrapper(set: DrinkwareSet) {
    override def toString = set.description + " (" + set.id + ")"
  }

  val undefinedIcon = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("net/scran24/admintool/warn.png")))
  undefinedIcon.setToolTipText("This parameter must be defined!")

  val initParam = params.map(p => (p.name, p.value)).toMap

  val drinkwareLabel = new JLabel("Drinkware set: ")
  val fillLevelLabel = new JLabel("Initial fill level: ")
  val skipFillLabel = new JLabel("Skip fill level prompt: ")

  drinkwareLabel.setPreferredSize(new Dimension(150, 20))
  fillLevelLabel.setPreferredSize(new Dimension(150, 20))
  skipFillLabel.setPreferredSize(new Dimension(150, 20))

  val drinkwareValue = new JLabel(initParam.get("drinkware-id").getOrElse("(undefined)"))
  drinkwareValue.setPreferredSize(new Dimension(100, 20))

  val initialValue = (initParam.get("initial-fill-level").map(_.toDouble).getOrElse(0.9) * 100.0).toInt

  val fillLevelValue = new JLabel(initialValue + "%")

  val fillLevel = new JSlider()
  fillLevel.setMinimum(0)
  fillLevel.setMaximum(100)
  fillLevel.setValue(initialValue)
  fillLevel.setMajorTickSpacing(10)
  fillLevel.setPreferredSize(new Dimension(150, 20))
  fillLevel.setOpaque(false)
  fillLevel.addChangeListener(new ChangeListener {
    override def stateChanged(e: ChangeEvent) = {
      fillLevelValue.setText(fillLevel.getValue + "%")
      changesMade()
    }
  })

  val skipFill = new JCheckBox()
  skipFill.setSelected(initParam.contains("skip-fill-level") && initParam("skip-fill-level").equals("true"))
  skipFill.addItemListener(new ItemListener { override def itemStateChanged(e: ItemEvent) = changesMade() })
  skipFill.setOpaque(false)

  val chooseSetButton = new JButton("Change...")

  chooseSetButton.addActionListener(() => {
    val dialog = new SelectionDialog[DrinkwareWrapper](ownerFrame(this), "Select a drinkware set", drinkwareDefs.map(DrinkwareWrapper(_)))
    dialog.setVisible(true)
    dialog.choice match {
      case Some(DrinkwareWrapper(set)) => {
        drinkwareValue.setText(set.id)
        changesMade()
      }
      case _ => {}
    }
  })

  add(drinkwareLabel)
  add(drinkwareValue)
  add(chooseSetButton)
  add(new LineBreak)
  add(fillLevelLabel)
  add(fillLevel)
  add(fillLevelValue)
  add(new LineBreak)
  add(skipFillLabel)
  add(skipFill)

  def parameters =
    Seq(PortionSizeMethodParameter("drinkware-id", drinkwareValue.getText),
      PortionSizeMethodParameter("initial-fill-level", (fillLevel.getValue / 100.0).toString),
      PortionSizeMethodParameter("skip-fill-level", if (skipFill.isSelected()) "true" else "false"))

  val methodName = "drink-scale"
}