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
import net.scran24.fooddef.Food
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
import net.scran24.fooddef.CategoryV2
import javax.swing.ImageIcon
import SwingUtil._
import net.scran24.fooddef.AsServedSet
import net.scran24.fooddef.GuideImage
import net.scran24.fooddef.DrinkwareSet
import net.scran24.fooddef.PortionSizeMethod
import javax.swing.JComboBox
import net.scran24.fooddef.FoodGroup
import java.awt.event.ItemListener
import java.awt.event.ItemEvent
import javax.swing.JCheckBox
import net.scran24.fooddef.InheritableAttributes
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import net.scran24.fooddef.FoodLocal
import java.util.UUID

class FoodDefPanel(portion: PortionSizeResolver, foods: MutableFoods, foodGroups: Seq[FoodGroup], categories: MutableCategories, portionRes: PortionResources,
  imageDirectory: ImageDirectory, food: Food, updateFood: (String, Food, Seq[CategoryV2]) => Boolean, deleteFood: Food => Unit) extends DefEditor {

  val codeText = new JTextField(food.code)
  addChangeListener(codeText, () => {
    val newCode = codeText.getText
    val existing = foods.find(newCode)
    if (existing.isDefined && food.code != newCode) {
      codeErrorIcon.setVisible(true)
      codeErrorIcon.setToolTipText("Code is already used by " + existing.get.englishDescription)
    } else
      codeErrorIcon.setVisible(false)

    changesMade()
  })
  
  val codeLabel = new JLabel("Intake24 code")
  val ddd = codeText.getPreferredSize()
  codeText.setPreferredSize(new Dimension(70, ddd.height))

  codeLabel.setPreferredSize(new Dimension(100, 20))

  val ndnsText = new JTextField(food.localData.nutrientTableCodes.getOrElse("NDNS", "").toString())
  addChangeListener(ndnsText, () => changesMade())
  val ndnsLabel = new JLabel("NDNS code")
  ndnsLabel.setPreferredSize(new Dimension(100, 20))
  val dd = ndnsText.getPreferredSize()
  ndnsText.setPreferredSize(new Dimension(70, dd.height))
  
  val nzText = new JTextField(food.localData.nutrientTableCodes.getOrElse("NZ", "").toString())
  addChangeListener(nzText, () => changesMade())
  val nzLabel = new JLabel("NZ code")
  nzLabel.setPreferredSize(new Dimension(100, 20))
  val dd2 = nzText.getPreferredSize()
  nzText.setPreferredSize(new Dimension(70, dd2.height))
  
  val groupCodeLabel = new JLabel ("Food group")
  groupCodeLabel.setPreferredSize(new Dimension(100, 20))
  val groupCode = new JComboBox(foodGroups.toArray)
  groupCode.setSelectedIndex(foodGroups.indexWhere(_.id == food.groupCode))
  groupCode.addItemListener(new ItemListener { def itemStateChanged(e: ItemEvent) = changesMade() })
  val d1 = groupCode.getPreferredSize()
  groupCode.setPreferredSize(new Dimension(270, d1.height))

  val descText = new JTextField(food.englishDescription)
  addChangeListener(descText, () => changesMade())
  val descLabel = new JLabel("Description")
  descLabel.setPreferredSize(new Dimension(100, 20))
  val d = descText.getPreferredSize()
  descText.setPreferredSize(new Dimension(300, d.height))
  
  val attrPanel = new EditableAttributesPanel(food.code, true, food.attributes, portion, changesMade)

  val headerPanel = new JPanel(new BorderLayout())

  val header = new JLabel("Food definition")
  header.setFont(new Font("Dialog", 0, 20))

  headerPanel.add(header, BorderLayout.WEST)
  val spacer = new JPanel()
  spacer.setPreferredSize(new Dimension(250, 20))

  headerPanel.add(spacer, BorderLayout.CENTER)

  val changeIcon = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("net/scran24/admintool/warn.png")))
  changeIcon.setToolTipText("<html><p>You have made some changes to this food.</p><p>Use the buttons on the bottom of this window to accept or discard changes</p>");
  changeIcon.setVisible(false)

  val codeErrorIcon = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("net/scran24/admintool/error.png")))
  codeErrorIcon.setVisible(false)

  headerPanel.add(changeIcon, BorderLayout.EAST)

  val catLabel = new JLabel("Categories")
  catLabel.setFont(new Font("Dialog", 0, 20))

  val changeLabel = new JLabel("Changes")
  changeLabel.setFont(new Font("Dialog", 0, 20))

  val deleteButton = new JButton("Delete this food")

  deleteButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: java.awt.event.ActionEvent) = deleteFood(food)
  })
  
  val copyButton = new JButton ("Copy from...")
  copyButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: java.awt.event.ActionEvent) = copyFrom()
  })

  val catPanelContainer = new JPanel()
  val portionSizePanelContainer = new JPanel()
  
  val catPanel = new CategoriesPanel(categories.foodSuperCategories(food.code).sortBy(_.description), categories, changesMade)
  var portionSizePanel = new PortionSizePanel(food.code, true, food.localData.portionSize, portionRes, imageDirectory, portion, changesMade)

  portionSizePanelContainer.add(portionSizePanel)
  
  add(headerPanel)
  add(new LineBreak)
  
  val superCats = categories.foodSuperCategories(food.code)
  
  val allCats = superCats.flatMap(c => categories.categorySuperCategories(c.code))
  
  if ((superCats ++ allCats).forall(_.isHidden)) {
    val warningLabel = new JLabel ("This food is hidden because all of its parent categories are hidden")
    warningLabel.setForeground(Color.RED);
    add (warningLabel)
    add(new LineBreak)
  }
      
  add(codeLabel)
  add(codeText)
  add(codeErrorIcon)
  add(new LineBreak)
  add(ndnsLabel)
  add(ndnsText)
  add(new LineBreak)
  add(nzLabel)
  add(nzText)  
  add(new LineBreak)
  add(groupCodeLabel)
  add(groupCode)
  add(new LineBreak)
  add(descLabel)
  add(descText)
  add(new LineBreak)
  add(attrPanel)
  add(new LineBreak)
  add(deleteButton)
  add(copyButton)
  add(new LineBreak)
  add(catLabel)
  add(new LineBreak)
  add(catPanel)
  add(new LineBreak)
  add(portionSizePanelContainer)
  add(new LineBreak)
  add(changeLabel)
  add(new LineBreak)
  
  def nutrientCodeSnapshot: Map[String, String] = {
    val ndns = ndnsText.getText.trim()
    val nz = nzText.getText.trim()
    
    var res = Map[String, String]()
    
    if (!ndns.isEmpty())
      res += "NDNS" -> ndns
    if (!nz.isEmpty())
      res += "NZ" -> nz
    
    res
  }

  def snapshot = new Food(UUID.randomUUID(), codeText.getText(), descText.getText(), groupCode.getSelectedItem().asInstanceOf[FoodGroup].id, 
      InheritableAttributes(attrPanel.readyMealAttr, attrPanel.sameAsBeforeAttr, attrPanel.reasonableAmountAttr ),
      FoodLocal(Some(UUID.randomUUID()), Some(descText.getText()), false, nutrientCodeSnapshot, portionSizePanel.portionSizes.map(_.portionSizeMethod)))

  def changesMade() = {
    changed = true
    changeIcon.setVisible(true)
    acceptButton.setEnabled(true)
    discardButton.setEnabled(true)
  }

  def acceptChanges() = {
    val ok = updateFood(food.code, snapshot, catPanel.categories)
    if (ok) changed = false
    ok
  }
  
  def copyFrom() = {
    val dialog = new SelectionDialog[SearchFoodWrapper](ownerFrame(this), "Select food", foods.snapshot().map(SearchFoodWrapper(_)))
    dialog.setVisible(true)
    dialog.choice.foreach(food => {
      ndnsText.setText(food.food.localData.nutrientTableCodes("NDNS"))
      portionSizePanelContainer.removeAll()
      portionSizePanel = new PortionSizePanel(codeText.getText(), true, food.food.localData.portionSize, portionRes, imageDirectory, portion, changesMade)
      portionSizePanelContainer.add(portionSizePanel)
      changesMade()
    })
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
          codeText.setText(food.code)
          ndnsText.setText(food.localData.nutrientTableCodes("NDNS"))
          descText.setText(food.englishDescription)
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