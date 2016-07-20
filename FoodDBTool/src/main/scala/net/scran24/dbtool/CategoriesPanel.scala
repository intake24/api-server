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
import uk.ac.ncl.openlab.intake24.CategoryV2
import org.workcraft.gui.SimpleFlowLayout
import java.awt.BorderLayout
import javax.swing.BorderFactory
import java.awt.Color
import javax.swing.JList
import javax.swing.DefaultListModel
import java.awt.FlowLayout
import javax.swing.JButton
import java.awt.Dimension
import SwingUtil._
import javax.swing.SwingUtilities
import javax.swing.JFrame
import javax.swing.event.ListSelectionListener
import javax.swing.event.ListSelectionEvent
import javax.swing.ListSelectionModel
import javax.swing.JScrollPane


class CategoriesPanel(initial: Seq[CategoryV2], allCategories: MutableCategories, onChangesMade: () => Unit) extends JPanel {
  setLayout(new BorderLayout())
  
  def discard() = {
    model.clear()
    initial.foreach(cat => model.addElement(CategoryWrapper(cat))) 
  }

  //setBorder(BorderFactory.createTitledBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black, 1), BorderFactory.createEmptyBorder(5, 5, 5, 5)), "Parent categories"))

  val model = new DefaultListModel[CategoryWrapper]()
  val catList = new JList[CategoryWrapper](model)

  discard()

  catList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  
  catList.addListSelectionListener(new ListSelectionListener {
    override def valueChanged(e: ListSelectionEvent) = remove.setEnabled(catList.getSelectedValue() != null)
  })

  val buttons = new JPanel(new FlowLayout(FlowLayout.LEADING))

  val remove = new JButton("Remove from selected category")
  remove.setEnabled(false)

  remove.addActionListener(() => {
    model.removeElement(catList.getSelectedValue())
    onChangesMade()
  }:Unit)

  buttons.add(remove)
  val add = new JButton("Add to a category...")
  buttons.add(add)

  add.addActionListener(() => {
    val dialog = new SelectionDialog[CategoryWrapper](ownerFrame(this), "Select a category", allCategories.snapshot.map(CategoryWrapper(_)))
    dialog.setVisible(true)
    dialog.choice match {
      case Some(wrapper) => {
        model.addElement(wrapper)
        onChangesMade()
      }
      case _ => {}
    }
  })

  val scroll = new JScrollPane()
  scroll.setViewportView(catList)
  
  add(scroll, BorderLayout.CENTER)
  add(buttons, BorderLayout.SOUTH)
  
  def categories = Range(0, model.getSize()).map(model.getElementAt(_).category)
  
  setPreferredSize(new Dimension(500, 120))
}