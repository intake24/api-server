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

import scala.language.implicitConversions

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import java.awt.Color
import java.awt.Container
import java.awt.Component
import java.awt.Frame
import javax.swing.SwingUtilities
import javax.swing.JFrame

object SwingUtil {
  implicit def mkActionListener(handler: ActionEvent => Unit) = new ActionListener { override def actionPerformed(e: ActionEvent) = handler(e) }
  implicit def mkActionListener(handler: () => Unit) = new ActionListener { override def actionPerformed(e: ActionEvent) = handler() }

  def addChangeListener(text: JTextField, handler: () => Unit) = text.getDocument().addDocumentListener(new DocumentListener {
    def changedUpdate(e: DocumentEvent): Unit = {}
    def insertUpdate(e: DocumentEvent): Unit = handler()
    def removeUpdate(e: DocumentEvent): Unit = handler()
  })

  def clamp(x: Int) = math.max(0, x)

  def slightlyDarker(c: Color) = new Color(clamp(c.getRed() - 20), clamp(c.getGreen() - 20), clamp(c.getBlue() - 20), c.getAlpha())

  def recursiveSetEnabled(c: Container, enabled: Boolean): Unit = {
    c.getComponents().foreach(comp => {
      comp.setEnabled(enabled)
      if (comp.isInstanceOf[Container]) recursiveSetEnabled(comp.asInstanceOf[Container], enabled)
    })
  }
  
  def ownerFrame(c: Component): JFrame = SwingUtilities.getAncestorOfClass(classOf[JFrame], c).asInstanceOf[JFrame]
}