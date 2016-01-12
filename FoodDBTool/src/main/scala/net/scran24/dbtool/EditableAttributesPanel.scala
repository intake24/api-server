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
import net.scran24.fooddef.InheritableAttributes
import javax.swing.JLabel
import javax.swing.JCheckBox
import net.scran24.fooddef.InheritableAttributes
import javax.swing.JButton
import SwingUtil._
import java.awt.event.ItemListener
import java.awt.event.ItemEvent
import net.scran24.fooddef.InheritableAttributes
import java.awt.FlowLayout
import java.awt.Insets
import java.awt.Dimension
import org.workcraft.gui.SimpleFlowLayout
import java.awt.SystemColor
import javax.swing.BorderFactory
import javax.swing.JTextField
import org.workcraft.gui.SimpleFlowLayout.LineBreak
import javax.swing.JOptionPane
import javax.swing.JFormattedTextField
import java.awt.Color
import javax.swing.SwingConstants
import javax.swing.event.DocumentListener
import javax.swing.event.DocumentEvent

class EditableAttributesPanel(code: String, food: Boolean, attr: InheritableAttributes, resolver: PortionSizeResolver, changesMade: () => Unit) extends JPanel {
  setLayout(new SimpleFlowLayout(400))
  setBackground(slightlyDarker(SystemColor.control))
  setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5))

  var readyMealAttr = attr.readyMealOption
  var sameAsBeforeAttr = attr.sameAsBeforeOption
  var reasonableAmountAttr = attr.reasonableAmount

  val linearisationLabel = new JLabel("<html>" + resolver.resolveInheritance(code).map(_.description).mkString("<br>") + "</html>")

  val readyMealLabel = new JLabel()
  val readyMealCheck = new JCheckBox()
  readyMealCheck.setOpaque(false)
  readyMealCheck.setPreferredSize(new Dimension(40, 20))
  readyMealCheck.setHorizontalAlignment(SwingConstants.CENTER)

  val readyMealCheckChangeListener = new ItemListener {
    def itemStateChanged(e: ItemEvent) = {
      readyMealAttr = Some(readyMealCheck.isSelected())
      changesMade()
    }
  }

  val readyMealOverrideButton = new JButton()
  readyMealOverrideButton.setMargin(new Insets(2, 2, 2, 2))
  readyMealOverrideButton.setFont(readyMealOverrideButton.getFont().deriveFont(10.0f))
  readyMealOverrideButton.setPreferredSize(new Dimension(80, 20))

  def updateReadyMealInterface() = {
    if (readyMealAttr.isDefined) {
      readyMealLabel.setText("Ready meal option")
      readyMealCheck.setSelected(readyMealAttr.get)
      readyMealCheck.setEnabled(true)
      readyMealCheck.addItemListener(readyMealCheckChangeListener)
      readyMealLabel.setEnabled(true)

      readyMealOverrideButton.setText("Use inherited")
    } else {
      readyMealLabel.setEnabled(false)
      readyMealCheck.setEnabled(false)
      readyMealCheck.removeItemListener(readyMealCheckChangeListener)

      val inherited = if (food)
        resolver.foodInheritedAttribute(code, (a: InheritableAttributes) => a.readyMealOption)
      else
        resolver.categoryInheritedAttribute(code, (a: InheritableAttributes) => a.readyMealOption)

      inherited match {
        case Some(value) => {
          readyMealLabel.setText("Ready meal option (inherited)")
          readyMealCheck.setSelected(value)
        }
        case None => {
          readyMealLabel.setText("Ready meal option (default)")
          readyMealCheck.setSelected(InheritableAttributes.readyMealDefault)
        }
      }

      readyMealOverrideButton.setText("Override")
    }
  }

  readyMealOverrideButton.addActionListener(() => {
    if (readyMealAttr.isDefined)
      readyMealAttr = None
    else
      readyMealAttr = Some(InheritableAttributes.readyMealDefault)

    changesMade()
    updateReadyMealInterface()
  })

  val sameAsBeforeLabel = new JLabel()
  val sameAsBeforeCheck = new JCheckBox()
  sameAsBeforeCheck.setOpaque(false)
  sameAsBeforeCheck.setPreferredSize(new Dimension(40, 20))
  sameAsBeforeCheck.setHorizontalAlignment(SwingConstants.CENTER)

  val sameAsBeforeCheckChangeListener = new ItemListener {
    def itemStateChanged(e: ItemEvent) = {
      sameAsBeforeAttr = Some(sameAsBeforeCheck.isSelected())
    }
  }

  val sameAsBeforeOverrideButton = new JButton()
  sameAsBeforeOverrideButton.setMargin(new Insets(2, 2, 2, 2))
  sameAsBeforeOverrideButton.setFont(sameAsBeforeOverrideButton.getFont().deriveFont(10.0f))
  sameAsBeforeOverrideButton.setPreferredSize(new Dimension(80, 20))

  def updateSameAsBeforeInterface() = {
    if (sameAsBeforeAttr.isDefined) {
      sameAsBeforeLabel.setText("Same as before option")
      sameAsBeforeCheck.setSelected(sameAsBeforeAttr.get)
      sameAsBeforeCheck.setEnabled(true)
      sameAsBeforeCheck.addItemListener(sameAsBeforeCheckChangeListener)
      sameAsBeforeLabel.setEnabled(true)

      sameAsBeforeOverrideButton.setText("Use inherited")
    } else {
      sameAsBeforeLabel.setEnabled(false)
      sameAsBeforeCheck.setEnabled(false)
      sameAsBeforeCheck.removeItemListener(sameAsBeforeCheckChangeListener)
      val inherited = if (food)
        resolver.foodInheritedAttribute(code, (a: InheritableAttributes) => a.sameAsBeforeOption)
      else
        resolver.categoryInheritedAttribute(code, (a: InheritableAttributes) => a.sameAsBeforeOption)

      inherited match {
        case Some(value) => {
          sameAsBeforeLabel.setText("Same as before option (inherited)")

          sameAsBeforeCheck.setSelected(value)
        }
        case None => {
          sameAsBeforeLabel.setText("Same as before option (default)")
          sameAsBeforeCheck.setSelected(InheritableAttributes.sameAsBeforeDefault)
          sameAsBeforeCheck.setEnabled(false)
        }
      }

      sameAsBeforeOverrideButton.setText("Override")
    }
  }

  sameAsBeforeOverrideButton.addActionListener(() => {
    if (sameAsBeforeAttr.isDefined)
      sameAsBeforeAttr = None
    else
      sameAsBeforeAttr = Some(InheritableAttributes.sameAsBeforeDefault)

    changesMade()
    updateSameAsBeforeInterface()
  })

  val reasonableAmountLabel = new JLabel()
  val reasonableAmountText = new JTextField()

  reasonableAmountText.setInputVerifier(new NumberInputVerifier(reasonableAmountText.getBorder(), BorderFactory.createLineBorder(Color.RED)))
  reasonableAmountText.setPreferredSize(new Dimension(40, 20))

  val reasonableAmountOverrideButton = new JButton()
  reasonableAmountOverrideButton.setMargin(new Insets(2, 2, 2, 2))
  reasonableAmountOverrideButton.setFont(reasonableAmountOverrideButton.getFont().deriveFont(10.0f))
  reasonableAmountOverrideButton.setPreferredSize(new Dimension(80, 20))

  val reasonableAmountChangeListener = new DocumentListener {
    def changedUpdate(e: DocumentEvent): Unit = {}
    def insertUpdate(e: DocumentEvent): Unit = {
      try {
        reasonableAmountAttr = Some(reasonableAmountText.getText().toInt)
      } catch {
        case e: NumberFormatException => {}
      }
      changesMade()
    }
    def removeUpdate(e: DocumentEvent): Unit = {
      try {
        reasonableAmountAttr = Some(reasonableAmountText.getText().toInt)
      } catch {
        case e: NumberFormatException => {}
      }

      changesMade()
    }
  }

  def updateReasonableAmountInterface() = {
    if (reasonableAmountAttr.isDefined) {
      reasonableAmountLabel.setText("Reasonable amount (g/ml)")
      reasonableAmountText.setText(reasonableAmountAttr.get.toString)
      reasonableAmountText.setEnabled(true)
      reasonableAmountLabel.setEnabled(true)
      reasonableAmountText.getDocument().addDocumentListener(reasonableAmountChangeListener)

      reasonableAmountOverrideButton.setText("Use inherited")
    } else {
      reasonableAmountLabel.setEnabled(false)
      reasonableAmountText.setEnabled(false)
      reasonableAmountText.getDocument().removeDocumentListener(reasonableAmountChangeListener)

      val inherited = if (food)
        resolver.foodInheritedAttribute(code, (a: InheritableAttributes) => a.reasonableAmount)
      else
        resolver.categoryInheritedAttribute(code, (a: InheritableAttributes) => a.reasonableAmount)
      if (inherited.isDefined) {
        reasonableAmountLabel.setText("Reasonable amount (g/ml) (inherited)")
        reasonableAmountText.setText(inherited.get.toString)
      } else {
        reasonableAmountLabel.setText("Reasonable amount (g/ml) (default)")
        reasonableAmountText.setText(InheritableAttributes.reasonableAmountDefault.toString)
      }

      reasonableAmountOverrideButton.setText("Override")
    }
  }

  reasonableAmountOverrideButton.addActionListener(() => {
    if (reasonableAmountAttr.isDefined)
      reasonableAmountAttr = None
    else
      reasonableAmountAttr = Some(InheritableAttributes.reasonableAmountDefault)

    changesMade()
    updateReasonableAmountInterface()
  })

  updateReasonableAmountInterface()
  updateSameAsBeforeInterface()
  updateReadyMealInterface()

  addChangeListener(reasonableAmountText, () => {
    changesMade()
  })

  setLayout(new SimpleFlowLayout(400))
  setBackground(slightlyDarker(SystemColor.control))
  setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5))

  // add(linearisationLabel)
  add(new LineBreak)
  add(readyMealOverrideButton)
  add(readyMealCheck)
  add(readyMealLabel)
  add(new LineBreak)
  add(sameAsBeforeOverrideButton)
  add(sameAsBeforeCheck)
  add(sameAsBeforeLabel)

  add(new LineBreak)
  add(reasonableAmountOverrideButton)
  add(reasonableAmountText)
  add(reasonableAmountLabel)
}