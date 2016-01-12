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
import javax.swing.JTable
import javax.swing.table.DefaultTableModel
import org.workcraft.gui.SimpleFlowLayout
import java.awt.BorderLayout
import javax.swing.JScrollPane
import java.awt.Dimension
import javax.swing.JCheckBox
import javax.swing.DefaultCellEditor
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.event.TableModelListener
import javax.swing.event.TableModelEvent
import uk.ac.ncl.openlab.intake24.foodxml.StandardUnitDef

class StandardPortionUnitTable(initial: Seq[StandardUnitDef], changesMade: () => Unit) extends JPanel {
  setLayout(new BorderLayout())
  setPreferredSize(new Dimension(370, 150))

  val model = new DefaultTableModel() {
    override def getColumnCount() = 3
    override def getColumnClass(col: Int) = col match {
      case 0 => classOf[String]
      case 1 => classOf[java.lang.Double]
      case 2 => classOf[java.lang.Boolean]
    }
  }

  val table = new JTable(model)
  val scroll = new JScrollPane(table)

  val checkBox = new JCheckBox()
  checkBox.setOpaque(false)

  model.setColumnCount(2)
  model.setColumnIdentifiers(Array[Object]("Unit name (plural)", "Unit weight (g)", "Omit food description"))

  table.getColumnModel().getColumn(0).setMaxWidth(120)
  table.getColumnModel().getColumn(1).setMaxWidth(110)
  table.getColumnModel().getColumn(0).setMinWidth(120)
  table.getColumnModel().getColumn(1).setMinWidth(110)
  table.getColumnModel().getColumn(0).setWidth(120)
  table.getColumnModel().getColumn(1).setWidth(110)

  initial.foreach(unit => model.addRow(Array[Object](unit.name, new java.lang.Double(unit.weight), new java.lang.Boolean(unit.omitFoodDesc))))

  model.addTableModelListener(new TableModelListener {
    override def tableChanged(e: TableModelEvent) = changesMade()
  })

  add(scroll, BorderLayout.CENTER)

  def addUnit() = {
    model.addRow(Array[Object]("New units", new java.lang.Double(1.0), new java.lang.Boolean(false)))
  }

  def delete() = table.getSelectedRow() match {
    case -1 => {}
    case i => model.removeRow(i)
  }
  
  def moveDown() = table.getSelectedRow() match {
    case -1 => {}
    case i if i < (model.getRowCount()-1) => {
      model.moveRow(i, i, i+1)
      table.setRowSelectionInterval(i+1, i+1)
    }
    case _ => {}
  }
  
  def moveUp() = table.getSelectedRow() match {
    case -1 => {}
    case i if i > 0 => {
      model.moveRow(i, i, i-1)
      table.setRowSelectionInterval(i-1, i-1)
    }
    case _ => {}
  }

  def units = {
    Range(0, model.getRowCount()).map(rowIndex => {
      val desc = model.getValueAt(rowIndex, 0).asInstanceOf[String]
      val weight = model.getValueAt(rowIndex, 1).asInstanceOf[java.lang.Double].doubleValue()
      val omitFoodDesc = model.getValueAt(rowIndex, 2).asInstanceOf[java.lang.Boolean].booleanValue()

      StandardUnitDef(desc, weight, omitFoodDesc)
    })
  }
}