/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import javax.swing.JDialog
import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.JList
import javax.swing.DefaultListModel
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JTextField
import javax.swing.JLabel
import javax.swing.ListSelectionModel
import SwingUtil._
import javax.swing.JScrollPane
import javax.swing.event.DocumentListener
import javax.swing.event.DocumentEvent
import javax.swing.JFrame
import javax.swing.event.ListSelectionListener
import javax.swing.event.ListSelectionEvent
import javax.swing.BorderFactory
import java.awt.event.MouseListener
import java.awt.event.MouseAdapter

class SelectionDialog[T](parent: JFrame, title: String, elements: Seq[T]) extends JDialog(parent) {
  setModal(true)
  setSize(500, 500)
  setTitle(title)
  setLocationRelativeTo(parent)

  var choice: Option[T] = None

  val contents = new JPanel(new BorderLayout(5, 5))

  val model = new DefaultListModel[T]()
  val lst = new JList(model)
  lst.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  lst.addListSelectionListener(new ListSelectionListener {
    override def valueChanged(e: ListSelectionEvent) = ok.setEnabled(lst.getSelectedValue() != null)
  })

  lst.addMouseListener(new MouseAdapter {
    override def mouseClicked(e: java.awt.event.MouseEvent) = if (e.getClickCount() == 2) {
      choice = Option(lst.getSelectedValue())
      setVisible(false)
    }
  })

  elements.foreach(model.addElement)

  val scroll = new JScrollPane()
  scroll.setViewportView(lst)

  contents.add(scroll, BorderLayout.CENTER)

  val buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT))

  val ok = new JButton("OK")
  ok.setEnabled(false)
  val cancel = new JButton("Cancel")

  buttons.add(ok)
  buttons.add(cancel)

  cancel.addActionListener(() => {
    choice = None
    setVisible(false)
  })

  ok.addActionListener(() => {
    choice = Option(lst.getSelectedValue())
    setVisible(false)
  })

  contents.add(buttons, BorderLayout.SOUTH)
  contents.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

  val searchPanel = new JPanel(new BorderLayout())
  val searchLabel = new JLabel("Filter: ")
  val searchText = new JTextField()

  addChangeListener(searchText, () => {
    val search = searchText.getText()
    model.clear()
    if (search.isEmpty)
      elements.foreach(model.addElement)
    else
      elements.filter(_.toString.toLowerCase().contains(search.toLowerCase())).foreach(model.addElement)
  })

  searchPanel.add(searchText, BorderLayout.CENTER)
  searchPanel.add(searchLabel, BorderLayout.WEST)

  contents.add(searchPanel, BorderLayout.NORTH)

  setContentPane(contents)
}