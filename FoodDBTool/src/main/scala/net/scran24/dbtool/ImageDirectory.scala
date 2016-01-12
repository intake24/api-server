/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import java.io.File
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.filechooser.FileFilter
import javax.swing.JOptionPane

case class ImageDirectory(dialogParent: JFrame, val root: String) {
  def exists(url: String) = new File(root + File.separator + url).exists()

  def chooseImage(): Option[String] = {
    val fileChooser = new JFileChooser()
    fileChooser.setDialogTitle("Choose an image")
    fileChooser.setCurrentDirectory(new File(root))
    fileChooser.setFileFilter(new FileFilter() {
      override def accept(f: File) =  f.isDirectory || f.getName().endsWith(".jpg") || f.getName().endsWith(".png")
      override def getDescription() = "Image files (*.jpg, *.png)";
    })

    if (fileChooser.showDialog(dialogParent, "Use this image") == JFileChooser.APPROVE_OPTION) {
      val path = fileChooser.getSelectedFile().getPath()
      
      if (!path.startsWith(root)) {
        JOptionPane.showMessageDialog(dialogParent, "Please choose an image from the Intake24 image directory", "Cannot use this image", JOptionPane.WARNING_MESSAGE)
        chooseImage()
      } else Some(path.substring(root.length() + File.separator.length()).replace(File.separator, "/"))
    } else None
  }
}