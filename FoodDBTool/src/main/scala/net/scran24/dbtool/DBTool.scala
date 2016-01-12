/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import javax.swing.JFrame
import javax.swing.SwingUtilities

object DBTool {
  def main(args: Array[String]) = {
    SwingUtilities.invokeLater(new Runnable {
      def run = {
        val mainWindow = new MainWindow()
        mainWindow.setVisible(true)
      }
    })
  }
}