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
import javax.swing.JTextField
import javax.swing.JComboBox
import javax.swing.JLabel
import java.awt.Dimension
import javax.swing.BorderFactory
import java.awt.Color
import net.scran24.fooddef.PortionSizeMethod
import org.workcraft.gui.SimpleFlowLayout.LineBreak
import javax.swing.JButton
import net.scran24.dbtool.portion.AsServedPortionSizeEditor
import java.awt.SystemColor
import net.scran24.fooddef.AsServedSet
import net.scran24.dbtool.portion.GuidePortionSizeEditor
import net.scran24.fooddef.GuideImage
import net.scran24.dbtool.portion.DrinkScalePortionSizeEditor
import net.scran24.fooddef.DrinkwareSet
import SwingUtil._
import Util._
import java.awt.BorderLayout
import java.awt.event.ItemListener
import java.awt.event.ItemEvent
import net.scran24.dbtool.portion.PortionSizeEditor
import javax.swing.ImageIcon
import javax.swing.SwingConstants
import net.scran24.dbtool.portion.StandardPortionSizeEditor
import java.awt.Insets
import net.scran24.dbtool.portion.CerealPortionSizeEditor
import net.scran24.dbtool.portion.PizzaPortionSizeEditor
import net.scran24.dbtool.portion.MilkOnCerealPortionSizeEditor
import net.scran24.dbtool.portion.MilkInHotDrinkPortionSizeEditor
import javax.swing.JCheckBox
import javax.swing.event.ChangeListener
import javax.swing.event.ChangeEvent
import net.scran24.fooddef.PortionSizeMethodParameter

class EditablePortionSizePanel(p: PortionSizeMethod, res: PortionResources, imageDirectory: ImageDirectory, delete: EditablePortionSizePanel => Unit, changesMade: () => Unit) extends JPanel {
  type MaybeParams = Option[Seq[PortionSizeMethodParameter]]

  var asServedParams: MaybeParams = None
  var guideParams: MaybeParams = None
  var drinkScaleParams: MaybeParams = None
  var standardPortionParams: MaybeParams = None
  var cerealPortionParams: MaybeParams = None
  var milkPortionParams: MaybeParams = None
  var milkHotDrinkPortionParams: MaybeParams = None
  var pizzaPortionParams: MaybeParams = None
  var currentEditor: Option[PortionSizeEditor] = None

  def saveParams() = currentEditor match {
    case Some(editor) => editor.methodName match {
      case "as-served" => asServedParams = Some(editor.parameters)
      case "guide-image" => guideParams = Some(editor.parameters)
      case "drink-scale" => drinkScaleParams = Some(editor.parameters)
      case "standard-portion" => standardPortionParams = Some(editor.parameters)
      case "cereal" => cerealPortionParams = Some(editor.parameters)
      case "milk-in-a-hot-drink" => milkHotDrinkPortionParams = Some(editor.parameters)
      case "milk-on-cereal" => milkPortionParams = Some(editor.parameters)
      case "pizza" => pizzaPortionParams = Some(editor.parameters)
    }
    case None => {}
  }

  def switchEditor(editor: PortionSizeEditor) = {
    if (currentEditor.isDefined) changesMade()
    saveParams()
    editorContainer.removeAll()
    editorContainer.add(editor)
    currentEditor = Some(editor)
    checkParams()
    revalidate()
    repaint()
  }

  def checkParams() = {
    val problems = currentEditor match {
      case Some(editor) => checkPortionSize(portionSizeMethod)
      case None => Seq()
    }

    if (problems.isEmpty)
      errorIcon.setVisible(false)
    else {
      errorIcon.setVisible(true)
      errorIcon.setToolTipText("<html><p>" + problems.mkString("</p><p>") + "</p></html>")
    }
  }

  def paramChangesMade() = {
    checkParams()
    changesMade()
  }

  def switchToAsServed() = switchEditor(new AsServedPortionSizeEditor(asServedParams.getOrElse(Seq()), res.asServedSets, paramChangesMade))
  def switchToGuide() = switchEditor(new GuidePortionSizeEditor(guideParams.getOrElse(Seq()), res.guideImages, paramChangesMade))
  def switchToDrinkScale() = switchEditor(new DrinkScalePortionSizeEditor(drinkScaleParams.getOrElse(Seq()), res.drinkwareSets, paramChangesMade))
  def switchToStandardPortion() = switchEditor(new StandardPortionSizeEditor(standardPortionParams.getOrElse(Seq()), paramChangesMade))
  def switchToCereal() = switchEditor(new CerealPortionSizeEditor(cerealPortionParams.getOrElse(Seq()), paramChangesMade))
  def switchToMilk() = switchEditor(new MilkOnCerealPortionSizeEditor(milkPortionParams.getOrElse(Seq()), paramChangesMade))
  def switchToMilkInHotDrink() = switchEditor(new MilkInHotDrinkPortionSizeEditor(milkHotDrinkPortionParams.getOrElse(Seq()), paramChangesMade))
  def switchToPizza() = switchEditor(new PizzaPortionSizeEditor(pizzaPortionParams.getOrElse(Seq()), paramChangesMade))

  setLayout(new SimpleFlowLayout(400))
  setBackground(slightlyDarker(SystemColor.control))
  setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5))

  val errorIcon = new JLabel(new ImageIcon(getClass().getClassLoader().getResource("net/scran24/admintool/error.png")))
  errorIcon.setVisible(false)

  val descLabel = new JLabel("Description: ")
  descLabel.setPreferredSize(new Dimension(120, 20))
  descLabel.setHorizontalAlignment(SwingConstants.RIGHT)

  val descText = new JTextField()
  descText.setPreferredSize(new Dimension(150, 20))
  descText.setText(p.description)
  addChangeListener(descText, () => changesMade())
  
  val useForRecipesLabel = new JLabel("Use for recipes: ")
  useForRecipesLabel.setPreferredSize(new Dimension(120, 20))
  useForRecipesLabel.setHorizontalAlignment(SwingConstants.RIGHT)
  
  val useForRecipes = new JCheckBox
  useForRecipes.setOpaque(false)
  useForRecipes.setSelected(p.useForRecipes)
  useForRecipes.addChangeListener(new ChangeListener() {
    def stateChanged(e: ChangeEvent) = changesMade()
  })
  

  val imageLabel = new JLabel("Image: ")
  imageLabel.setPreferredSize(new Dimension(120, 20))
  imageLabel.setHorizontalAlignment(SwingConstants.RIGHT)

  val chooseImageButton = new JButton("Choose...")
  chooseImageButton.setMargin(new Insets(2, 2, 2, 2))
  chooseImageButton.setFont(chooseImageButton.getFont().deriveFont(10.0f))
  chooseImageButton.setPreferredSize(new Dimension(60, 20))
  chooseImageButton.addActionListener(() => {
    imageDirectory.chooseImage().foreach(img => imageUrl.setText(img))
  })

  val imageUrl = new JTextField()
  imageUrl.setPreferredSize(new Dimension(150, 20))
  imageUrl.setText(p.imageUrl)
  addChangeListener(imageUrl, () => changesMade())

  val methodLabel = new JLabel("Estimation method: ")
  methodLabel.setHorizontalAlignment(SwingConstants.RIGHT)
  methodLabel.setPreferredSize(new Dimension(120, 20))

  val method = new JComboBox(Array("As served", "Guide", "Drink scale", "Standard portion", "Cereal", "Milk on cereal", "Milk in a hot drink", "Pizza"))

  add(descLabel)
  add(descText)

  add(new LineBreak)

  add(imageLabel)
  add(imageUrl)
  add(chooseImageButton)

  add(new LineBreak)
      
  add(useForRecipesLabel)
  add(useForRecipes)
  
  add(new LineBreak)


  add(methodLabel)
  add(method)
  add(errorIcon)

  add(new LineBreak)

  val editorContainer = new JPanel(new BorderLayout())

  add(editorContainer)
  add(new LineBreak)

  p.method match {
    case "as-served" => {
      method.setSelectedIndex(0)
      asServedParams = Some(p.parameters)
      switchToAsServed()
    }
    case "guide-image" => {
      method.setSelectedIndex(1)
      guideParams = Some(p.parameters)
      switchToGuide()

    }
    case "drink-scale" => {
      method.setSelectedIndex(2)
      drinkScaleParams = Some(p.parameters)
      switchToDrinkScale()
    }
    case "standard-portion" => {
      method.setSelectedIndex(3)
      standardPortionParams = Some(p.parameters)
      switchToStandardPortion()
    }
    case "cereal" => {
      method.setSelectedIndex(4)
      cerealPortionParams = Some(p.parameters)
      switchToCereal()
    }
    case "milk-on-cereal" => {
      method.setSelectedIndex(5)
      milkPortionParams = Some(p.parameters)
      switchToMilk()      
    }
    case "milk-in-a-hot-drink" => {
      method.setSelectedIndex(6)
      milkHotDrinkPortionParams = Some(p.parameters)
      switchToMilkInHotDrink()      
    }
    case "pizza" => {
      method.setSelectedIndex(7)
      pizzaPortionParams = Some(p.parameters)
      switchToPizza()
    }
    case _ => {}
  }

  method.addItemListener(new ItemListener {
    override def itemStateChanged(e: ItemEvent) = method.getSelectedIndex() match {
      case 0 => switchToAsServed()
      case 1 => switchToGuide()
      case 2 => switchToDrinkScale()
      case 3 => switchToStandardPortion()
      case 4 => switchToCereal()
      case 5 => switchToMilk()
      case 6 => switchToMilkInHotDrink()
      case 7 => switchToPizza()
    }
  })

  val deleteButton = new JButton("Delete this method")
  deleteButton.addActionListener(() => {
    delete(this)
  })

  add(new LineBreak)
  add(deleteButton)

  checkParams()

  def portionSizeMethod = {
    val editor = currentEditor.get
    PortionSizeMethod(editor.methodName, descText.getText(), imageUrl.getText(), useForRecipes.isSelected(), editor.parameters)
  }
}