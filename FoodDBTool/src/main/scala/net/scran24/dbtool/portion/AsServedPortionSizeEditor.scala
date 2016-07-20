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
import net.scran24.dbtool.SelectionDialog
import net.scran24.dbtool.Util._
import java.awt.SystemColor
import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter

class AsServedPortionSizeEditor(params: Seq[PortionSizeMethodParameter], asServedSets: Seq[AsServedSet], changesMade: () => Unit) extends PortionSizeEditor {
  case class AsServedWrapper(set: AsServedSet) {
    override def toString = set.description + " (" + set.id + ")"
  }

  val undefinedIcon = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("net/scran24/admintool/warn.png")))
  undefinedIcon.setToolTipText("This parameter must be defined!")

  val initParam = params.map(p => (p.name, p.value)).toMap

  val servingLabel = new JLabel("Serving image set: ")
  val leftoversLabel = new JLabel("Leftovers image set: ")
  val useLeftoversLabel = new JLabel("Use leftover images: ")

  servingLabel.setPreferredSize(new Dimension(150, 20))
  leftoversLabel.setPreferredSize(new Dimension(150, 20))
  useLeftoversLabel.setPreferredSize(new Dimension(150, 20))

  val servingValue = new JLabel(initParam.get("serving-image-set").getOrElse("(undefined)"))
  val leftoversValue = new JLabel(initParam.get("leftovers-image-set").getOrElse("(undefined)"))
  val useLeftovers = new JCheckBox()

  def checkLeftovers() = {
    if (useLeftovers.isSelected()) {
      chooseLeftoversButton.setEnabled(true)
      leftoversLabel.setEnabled(true)
      leftoversValue.setEnabled(true)
    } else {
      chooseLeftoversButton.setEnabled(false)
      leftoversLabel.setEnabled(false)
      leftoversValue.setEnabled(false)
    }
  }

  useLeftovers.addActionListener(() => { checkLeftovers; changesMade() })
  useLeftovers.setOpaque(false)

  useLeftovers.setSelected(initParam.contains("leftovers-image-set"))

  servingValue.setPreferredSize(new Dimension(100, 20))
  leftoversValue.setPreferredSize(new Dimension(100, 20))

  val chooseServingButton = new JButton("Change...")

  chooseServingButton.addActionListener(() => {
    val dialog = new SelectionDialog[AsServedWrapper](ownerFrame(this), "Select an as served image set", asServedSets.map(AsServedWrapper(_)))
    dialog.setVisible(true)
    dialog.choice match {
      case Some(AsServedWrapper(set)) => {
        servingValue.setText(set.id)
        changesMade()
      }
      case _ => {}
    }
  })

  val chooseLeftoversButton = new JButton("Change...")

  chooseLeftoversButton.addActionListener(() => {
    val dialog = new SelectionDialog[AsServedWrapper](ownerFrame(this), "Select an as served image set", asServedSets.map(AsServedWrapper(_)))
    dialog.setVisible(true)
    dialog.choice match {
      case Some(AsServedWrapper(set)) => {
        leftoversValue.setText(set.id)
        changesMade()
      }
      case _ => {}
    }
  })

  checkLeftovers()

  add(servingLabel)
  add(servingValue)
  add(chooseServingButton)
  add(new LineBreak)
  add(useLeftoversLabel)
  add(useLeftovers)
  add(new LineBreak)
  add(leftoversLabel)
  add(leftoversValue)
  add(chooseLeftoversButton)

  def parameters =
    Seq(PortionSizeMethodParameter("serving-image-set", servingValue.getText)) ++ conditional(useLeftovers.isSelected(), PortionSizeMethodParameter("leftovers-image-set", leftoversValue.getText)).toSeq

  val methodName = "as-served"
}