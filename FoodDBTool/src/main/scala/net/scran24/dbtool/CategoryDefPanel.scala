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

import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JTextField
import java.awt.BorderLayout
import java.awt.GridLayout
import org.workcraft.gui.SimpleFlowLayout
import org.workcraft.gui.SimpleFlowLayout.LineBreak
import java.awt.Dimension
import java.awt.Font
import javax.swing.JButton
import java.awt.Color
import java.awt.event.ActionListener
import javax.swing.BorderFactory
import java.awt.event.ActionEvent
import javax.swing.JOptionPane
import uk.ac.ncl.openlab.intake24.CategoryV2
import javax.swing.ImageIcon
import SwingUtil._
import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import javax.swing.JCheckBox
import java.awt.event.ItemListener
import java.awt.event.ItemEvent
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.foodxml.CategoryDef
import java.util.UUID

class CategoryDefPanel(portion: PortionSizeResolver, foods: MutableFoods, categories: MutableCategories, portionResources: PortionResources, imageDirectory: ImageDirectory,
  category: CategoryV2, updateCategory: (String, CategoryV2, Seq[CategoryV2]) => Boolean, delete: CategoryV2 => Unit) extends DefEditor {

  val codeText = new JTextField(category.code)
  addChangeListener(codeText, () => {
    val newCode = codeText.getText
    val existing = categories.find(newCode)
    if (existing.isDefined && category.code != newCode) {
      codeErrorIcon.setVisible(true)
      codeErrorIcon.setToolTipText("Code is already used by " + existing.get.description)
    } else
      codeErrorIcon.setVisible(false)

    changesMade()
  })

  val codeLabel = new JLabel("Intake24 code")
  val ddd = codeText.getPreferredSize()
  codeText.setPreferredSize(new Dimension(70, ddd.height))

  codeLabel.setPreferredSize(new Dimension(100, 20))

  val descText = new JTextField(category.description)
  addChangeListener(descText, () => changesMade())
  val descLabel = new JLabel("Description")
  descLabel.setPreferredSize(new Dimension(100, 20))
  val d = descText.getPreferredSize()
  descText.setPreferredSize(new Dimension(300, d.height))
  
  val hiddenLabel = new JLabel("Hidden")
  val d4 = hiddenLabel.getPreferredSize()
  hiddenLabel.setPreferredSize(new Dimension(100, d4.height))
  
  val hiddenCheckbox = new JCheckBox()
  hiddenCheckbox.setSelected(category.isHidden)
  hiddenCheckbox.addItemListener(new ItemListener { override def itemStateChanged(e: ItemEvent) = changesMade() })

  val headerPanel = new JPanel(new BorderLayout())

  val header = new JLabel("Category definition")
  header.setFont(new Font("Dialog", 0, 20))

  headerPanel.add(header, BorderLayout.WEST)
  val spacer = new JPanel()
  spacer.setPreferredSize(new Dimension(250, 20))

  headerPanel.add(spacer, BorderLayout.CENTER)

  val changeIcon = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("net/scran24/admintool/warn.png")))
  changeIcon.setToolTipText("<html><p>You have made some changes to this category.</p><p>Use the buttons on the bottom of this window to accept or discard changes</p>");
  changeIcon.setVisible(false)

  val codeErrorIcon = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("net/scran24/admintool/error.png")))
  codeErrorIcon.setVisible(false)

  headerPanel.add(changeIcon, BorderLayout.EAST)

  val catLabel = new JLabel("Parent categories")
  catLabel.setFont(new Font("Dialog", 0, 20))

  val portionLabel = new JLabel("Portion size estimation")
  portionLabel.setFont(new Font("Dialog", 0, 20))

  val changeLabel = new JLabel("Changes")
  changeLabel.setFont(new Font("Dialog", 0, 20))

  val deleteButton = new JButton("Delete this category")

  val deleteMissing = new JButton("Delete references to missing foods/subcategories")

  deleteButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: java.awt.event.ActionEvent) = delete(category)
  })

  val missingFoods = category.foods.filter(foods.find(_).isEmpty)
  val missingSubcategories = category.subcategories.filter(categories.find(_).isEmpty)

  deleteMissing.addActionListener(new ActionListener() {
    override def actionPerformed(e: java.awt.event.ActionEvent) = {
      if (JOptionPane.showConfirmDialog(outer, "Are you sure you want to delete all references to missing category and/or food codes?\nThis will require applying all other changes you have made to this category as well.", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        val snap = snapshot
        val delFoods = snapshot.copy(foods = snapshot.foods.filterNot(missingFoods.contains((_))))
        val delSubc = delFoods.copy(subcategories = delFoods.subcategories.filterNot(missingSubcategories.contains(_)))
        val ok = updateCategory(category.code, delSubc, catPanel.categories)
        if (ok) changed = false
      }
    }
  })

  deleteMissing.setEnabled(!(missingFoods.isEmpty && missingSubcategories.isEmpty))

  val catPanel = new CategoriesPanel(categories.categorySuperCategories(category.code).sortBy(_.description), categories, changesMade)
  val portionSizePanel = new PortionSizePanel(category.code, false, category.portionSizeMethods, portionResources, imageDirectory, portion, changesMade)
val attrPanel = new EditableAttributesPanel(category.code, false, category.attributes, portion, changesMade)
  
  
  add(headerPanel)
  add(new LineBreak)
  add(codeLabel)
  add(codeText)
  add(codeErrorIcon)
  add(new LineBreak)
  add(hiddenLabel)
  add(hiddenCheckbox)
  add(new LineBreak)  
  add(descLabel)
  add(descText)
  add(new LineBreak)
  add(attrPanel)
  add(new LineBreak)
  add(deleteButton)
  add(new LineBreak)
  add(deleteMissing)
  add(new LineBreak)
  add(catLabel)
  add(new LineBreak)
  add(catPanel)
  add(new LineBreak)
  add(portionSizePanel)
  add(new LineBreak)
  add(changeLabel)
  add(new LineBreak)

  def snapshot = new CategoryV2(UUID.randomUUID(), codeText.getText(), descText.getText(), category.foods, category.subcategories, hiddenCheckbox.isSelected(),  
      InheritableAttributes(attrPanel.readyMealAttr, attrPanel.sameAsBeforeAttr, attrPanel.reasonableAmountAttr ), portionSizePanel.portionSizes.map(_.portionSizeMethod))

  def changesMade() = {
    changed = true
    changeIcon.setVisible(true)
    acceptButton.setEnabled(true)
    discardButton.setEnabled(true)
  }

  def acceptChanges() = {
    val ok = updateCategory(category.code, snapshot, catPanel.categories)
    if (ok) changed = false
    ok
  }

  def description = descText.getText()

  val acceptButton = new JButton("Accept changes")
  acceptButton.addActionListener(new ActionListener() { override def actionPerformed(e: ActionEvent) = acceptChanges() })
  acceptButton.setEnabled(false)
  add(acceptButton)

  val discardButton = new JButton("Discard changes")
  discardButton.setEnabled(false)

  val outer = this

  discardButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: ActionEvent) =
      {
        if (JOptionPane.showConfirmDialog(outer, "Are you sure you want do discard all changes to this food?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
          codeText.setText(category.code)
          descText.setText(category.description)
          catPanel.discard()
          portionSizePanel.discard()
          revalidate()
          repaint()
          changed = false
          changeIcon.setVisible(false)
        }
      }
  })

  add(discardButton)
}