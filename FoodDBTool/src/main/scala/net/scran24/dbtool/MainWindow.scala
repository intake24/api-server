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

import javax.swing.JFrame
import javax.swing.JTabbedPane
import javax.swing.JButton
import scala.xml.XML
import java.awt.Frame
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.KeyStroke
import javax.swing.JMenuItem
import javax.swing.JFileChooser
import SwingUtil._
import java.io.File
import java.io.PrintWriter
import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.AsServedSetV1
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.CategoryV2
import javax.swing.JOptionPane
import java.awt.event.InputEvent
import javax.swing.WindowConstants
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.FileNotFoundException
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileFilter
import au.com.bytecode.opencsv.CSVWriter
import java.io.FileWriter
import scala.collection.JavaConversions._

import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import uk.ac.ncl.openlab.intake24.foodxml.GuideImageDef
import uk.ac.ncl.openlab.intake24.foodxml.FoodGroupDef
import uk.ac.ncl.openlab.intake24.foodxml.DrinkwareDef
import uk.ac.ncl.openlab.intake24.foodxml.CategoryDef
import uk.ac.ncl.openlab.intake24.foodxml.AsServedDef
import uk.ac.ncl.openlab.intake24.foodxml.XmlFoodGroup
import uk.ac.ncl.openlab.intake24.foodxml.XmlCategoryRecord
import uk.ac.ncl.openlab.intake24.foodxml.XmlFoodRecord

case class Intake24Data(foods: Seq[XmlFoodRecord], foodGroups: Seq[XmlFoodGroup], categories: Seq[XmlCategoryRecord], asServedSets: Seq[AsServedSetV1], guideImages: Seq[GuideImage], drinkwareSets: Seq[DrinkwareSet])

case class Intake24DataPaths(foods: String, foodGroups: String, categories: String, asServedSets: String, guideImages: String, drinkwareSets: String)

class MainWindow extends JFrame("Intake24 database tool") {
  setSize(1000, 700)
  setLocationRelativeTo(null)
  setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
  //setExtendedState( getExtendedState() | Frame.MAXIMIZED_BOTH )

  val configDir = new File(System.getProperty("user.home") + File.separator + ".intake24")
  val configFile = new File(System.getProperty("user.home") + File.separator + ".intake24" + File.separator + "config")

  var dataPath: Option[String] = None
  var imagePath: Option[String] = None
  var foodEditor: Option[FoodEditor] = None

  def mkPaths(base: String) = {
    val paths = Seq("foods.xml", "food-groups.xml", "categories.xml", "as-served.xml", "guide.xml", "drinkware.xml").map(base + File.separator + _)
    Intake24DataPaths(paths(0), paths(1), paths(2), paths(3), paths(4), paths(5))
  }

  def loadData(paths: Intake24DataPaths) = {
    val foods = FoodDef.parseXml(XML.load(paths.foods))
    val foodGroups = FoodGroupDef.parseXml(XML.load(paths.foodGroups))
    val categories = CategoryDef.parseXml(XML.load(paths.categories))
    val asServedSets = AsServedDef.parseXml(XML.load(paths.asServedSets)).values.toSeq.sortBy(_.description)
    val guideImages = GuideImageDef.parseXml(XML.load(paths.guideImages)).values.toSeq.sortBy(_.description)
    val drinkwareSets = DrinkwareDef.parseXml(XML.load(paths.drinkwareSets)).values.toSeq.sortBy(_.description)

    Intake24Data(foods, foodGroups, categories, asServedSets, guideImages, drinkwareSets)
  }

  def readConfig() = {
    if (configFile.exists()) {
      val lines = scala.io.Source.fromFile(configFile).getLines.toSeq

      dataPath = if (lines(0).isEmpty()) None else Some(lines(0))
      imagePath = if (lines.length < 2 || lines(1).isEmpty()) None else Some(lines(1))
    }
  }

  def writeConfig() = {
    if (!configDir.exists())
      configDir.mkdir()

    val writer = new PrintWriter(configFile)

    writer.println(dataPath.getOrElse(""))
    writer.println(imagePath.getOrElse(""))

    writer.close()
  }

  def chooseDataPath() = {
    val fileChooser = new JFileChooser()
    fileChooser.setDialogTitle("Choose a directory that contains Intake24 data files")
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
    if (fileChooser.showDialog(this, "Use as data directory") == JFileChooser.APPROVE_OPTION) {
      val path = fileChooser.getSelectedFile().getPath()
      dataPath = Some(path)
      writeConfig()
      reload()
    }
  }

  def chooseImagePath() = {
    val fileChooser = new JFileChooser()
    fileChooser.setDialogTitle("Choose a directory that contains Intake24 image files")
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
    if (fileChooser.showDialog(this, "Use as image directory") == JFileChooser.APPROVE_OPTION) {
      val path = fileChooser.getSelectedFile().getPath()
      imagePath = Some(path)
      writeConfig()
      reload()
    }
  }

  def tryToLoadData(paths: Intake24DataPaths, imageDir: String) =
    if (!new File(imageDir).exists()) {
      showNoImagePanel()
    } else try {
      showEditor(loadData(paths), imageDir)
    } catch {
      case e: Throwable => {
        e.printStackTrace()
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
        showNoDataPanel()
      }
    }

  def showEditor(data: Intake24Data, imageDir: String) {
    val editor = new FoodEditor(data.foods, data.foodGroups, data.categories, PortionResources(data.asServedSets, data.guideImages, data.drinkwareSets), ImageDirectory(this, imageDir))
    foodEditor = Some(editor)
    val tabs = new JTabbedPane
    tabs.add("Foods", editor)
    setContentPane(tabs)
    reloadMenuItem.setEnabled(true)
    saveMenuItem.setEnabled(true)
    validate()
    repaint()
  }

  def showPanel[T <: JPanel](panel: T) {
    setContentPane(panel)
    reloadMenuItem.setEnabled(false)
    saveMenuItem.setEnabled(false)
    validate()
    repaint()
    //panel.paintImmediately(0, 0, panel.getWidth(), panel.getHeight())
  }

  def showNoDataPanel() {
    showPanel(new NoDataPanel())
  }

  def showNoImagePanel() {
    showPanel(new NoImagePanel())
  }

  def showLoadingPanel() {
    showPanel(new LoadingPanel())
  }

  def export(): Unit =
    for (
      editor <- foodEditor
    ) yield {
      val fileChooser = new JFileChooser()
      fileChooser.setDialogTitle("Export food list")
      fileChooser.setFileFilter(new FileFilter {
        override def accept(f: File) = f.isDirectory() || f.getName().endsWith(".csv")
        override def getDescription() = "Comma-separated value files (.csv)"
      })

      if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        val writer = new CSVWriter(new FileWriter(fileChooser.getSelectedFile()))
        val lines = Array("Intake24 code", "Food description", "NDNS code") +: editor.mutableFoods.snapshot.sortBy(_.description).map(f => Array(f.code, f.description, f.nutrientTableCodes("NDNS"))) 
        writer.writeAll(lines)
        writer.close()
      }
    }

  def save(): Unit =
    for (
      editor <- foodEditor;
      path <- dataPath
    ) yield {
      val paths = mkPaths(path)
      try {
        if (editor.acceptChanges()) {
          uk.ac.ncl.openlab.intake24.foodxml.Util.writeXml(FoodDef.toXml(editor.mutableFoods.snapshot), paths.foods)
          uk.ac.ncl.openlab.intake24.foodxml.Util.writeXml(CategoryDef.toXml(editor.mutableCategories.snapshot), paths.categories)
          editor.changed = false
        }
      } catch {
        case e: Throwable => JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE)
      }
    }

  def exit() = {
    System.exit(0);
  }

  def reload() =
    (dataPath, imagePath) match {
      case (None, _) => showNoDataPanel()
      case (_, None) => showNoImagePanel()
      case (Some(dataPath), Some(imagePath)) => {
        showLoadingPanel()

        SwingUtilities.invokeLater(new Runnable() {
          override def run() = {
            tryToLoadData(mkPaths(dataPath), imagePath)
          }
        });
      }
    }

  def createMenuBar() = {
    val file = new JMenu("File")
    file.setMnemonic('F')

    val reloadItem = new JMenuItem("Reload data")
    reloadItem.setMnemonic('r')
    reloadItem.setAccelerator(KeyStroke.getKeyStroke('R', InputEvent.CTRL_DOWN_MASK))
    reloadItem.addActionListener(() => reload())

    val exportItem = new JMenuItem("Export food list...")
    exportItem.setMnemonic('e')
    exportItem.setAccelerator(KeyStroke.getKeyStroke('E', InputEvent.CTRL_DOWN_MASK))
    exportItem.addActionListener(() => export())

    val exitItem = new JMenuItem("Exit")
    exitItem.setMnemonic('x')
    exitItem.addActionListener(() => exit())

    val saveItem = new JMenuItem("Save")
    saveItem.setMnemonic('s')
    saveItem.setAccelerator(KeyStroke.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK))
    saveItem.addActionListener(() => save())

    file.add(reloadItem)
    file.add(saveItem)
    file.add(exportItem)
    file.addSeparator()
    file.add(exitItem)

    val settings = new JMenu("Settings")
    settings.setMnemonic('S')
    val paths = new JMenuItem("Set data location...")
    paths.setAccelerator(KeyStroke.getKeyStroke('O', InputEvent.CTRL_DOWN_MASK))
    paths.addActionListener(() => chooseDataPath())
    settings.add(paths)

    val imgpath = new JMenuItem("Set image location...")
    imgpath.setAccelerator(KeyStroke.getKeyStroke('I', InputEvent.CTRL_DOWN_MASK))
    imgpath.addActionListener(() => chooseImagePath())
    settings.add(imgpath)

    val menu = new JMenuBar()
    menu.add(file)
    menu.add(settings)
    setJMenuBar(menu)

    (reloadItem, saveItem)
  }

  val (reloadMenuItem, saveMenuItem) = createMenuBar()

  val outer = this

  addWindowListener(new WindowAdapter() {
    override def windowClosing(e: WindowEvent) = foodEditor match {
      case Some(editor) => if (editor.areChangesMade()) {
        JOptionPane.showConfirmDialog(outer, "There are unsaved changes to the food database.\n\nDo you want to save them before exiting?", "Confirm exit", JOptionPane.YES_NO_CANCEL_OPTION) match {
          case JOptionPane.YES_OPTION => { if (editor.acceptChanges()) { save(); System.exit(0) } }
          case JOptionPane.NO_OPTION => { System.exit(0) }
          case JOptionPane.CANCEL_OPTION => {}
        }
      } else System.exit(0)
      case None => System.exit(0)
    }
  })

  readConfig()
  reload()
}