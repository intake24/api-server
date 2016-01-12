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

import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeModel
import javax.swing.JPanel
import java.awt.BorderLayout
import net.scran24.fooddef.Food
import net.scran24.fooddef.CategoryV2
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.ToolTipManager
import javax.swing.event.TreeSelectionListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.DefaultTreeSelectionModel
import java.awt.Font

sealed trait FoodTreeNode

case object Uncategorised extends FoodTreeNode {
  override def toString = "Uncategorised foods"
}

case class CategoryWrapper(category: CategoryV2) extends FoodTreeNode {
  override def toString = category.description
}

case class FoodWrapper(food: Food) extends FoodTreeNode {
  override def toString = food.englishDescription
}
case object RootNode extends FoodTreeNode {
  override def toString = "All foods"
}

class CellRenderer(problems: FoodTreeNode => Seq[String]) extends DefaultTreeCellRenderer {
  val okIcon = new ImageIcon(classOf[CellRenderer].getClassLoader().getResource("net/scran24/admintool/ok.png"))
  val errorIcon = new ImageIcon(classOf[CellRenderer].getClassLoader().getResource("net/scran24/admintool/error.png"))
  val normalFont = new Font("Dialog", Font.PLAIN, 12)
  val hiddenCategoryFont = new Font("Dialog", Font.ITALIC, 12)

  def setIcon(icon: ImageIcon) = {
    setLeafIcon(icon)
    setOpenIcon(icon)
    setClosedIcon(icon)
  }

  override def getTreeCellRendererComponent(tree: JTree, value: Object, sel: Boolean, expanded: Boolean, leaf: Boolean,
    row: Int, hasFocus: Boolean) = {

    val node = value.asInstanceOf[DefaultMutableTreeNode].getUserObject().asInstanceOf[FoodTreeNode]

    val probs = problems(node)

    if (probs.isEmpty)
      setIcon(okIcon)
    else
      setIcon(errorIcon)
    
    setFont(normalFont)
      
    node match {
      case RootNode => setIcon(null)
      case CategoryWrapper(cat) if cat.isHidden => setFont(hiddenCategoryFont)
      case _ => {  }
    }

    val component = super.getTreeCellRendererComponent(tree, value, sel,
      expanded, leaf, row, hasFocus);

    if (!probs.isEmpty) {
      val truncated = if (probs.length > 10)
        probs.take(10) :+ (probs.length - 10) + " more ..."
      else probs
      component.asInstanceOf[JLabel].setToolTipText("<html>" + truncated.mkString("<br/>") + "</html>")
    } else
      component.asInstanceOf[JLabel].setToolTipText(null);

    component
  }
}

class FoodTree(foods: MutableFoods, categories: MutableCategories, portionSize: PortionSizeResolver, select: Option[FoodTreeNode] => Unit, canChangeSelection: () => Boolean) extends JPanel {
  setLayout(new BorderLayout)

  val root = new DefaultMutableTreeNode(RootNode)

  var nodes = Map[String, List[DefaultMutableTreeNode]]().withDefaultValue(List())

  def categoryUpdated(code: String) = {
    nodes(code).foreach(n => {
      n.setUserObject(CategoryWrapper(categories.find(code).get))
      model.reload(n)
    })
  }

  def createFoodNode(food: Food) = {
    val node = new DefaultMutableTreeNode(FoodWrapper(food))
    nodes += (food.code -> (node :: nodes(food.code)))
    node
  }
  
  def isCategoryNode (node: DefaultMutableTreeNode) =
    node.getUserObject().isInstanceOf[CategoryWrapper]
  
  def insertBefore (node: DefaultMutableTreeNode, nodeToInsert: DefaultMutableTreeNode) = {
    if (node.toString == "Uncategorised foods") false
    else (isCategoryNode(node), isCategoryNode(nodeToInsert)) match {
      case (true, true) => node.toString() > nodeToInsert.toString()
      case (false, false) => node.toString() > nodeToInsert.toString()
      case (true, false) => false
      case (false, true) => false
    }
  }

  def insertNode(parent: DefaultMutableTreeNode, child: DefaultMutableTreeNode) = {
    val c = model.getChildCount(parent)
    val index = Range(0, c).find(i => insertBefore(model.getChild(parent, i).asInstanceOf[DefaultMutableTreeNode], child)).getOrElse(c)
    model.insertNodeInto(child, parent, index)
  }

  def selectEntry(code: String) = {
    val node = nodes(code).head
    val path = new TreePath(model.getPathToRoot(node).map(_.asInstanceOf[Object]))
    tree.makeVisible(path)
    tree.scrollPathToVisible(path)
    tree.getSelectionModel().setSelectionPath(path)
  }

  def entryAdded(code: String, superCats: Seq[CategoryV2], node: => DefaultMutableTreeNode) = {
    superCats.foreach(cat => {
      nodes(cat.code).foreach(supCatNode => {
        insertNode(supCatNode, node)
        model.nodeChanged(supCatNode)
      })
    })

    selectEntry(code)
  }

  def foodAdded(food: Food): Unit = {
    def node = createFoodNode(food)
    
    val superCats = categories.foodSuperCategories(food.code)
    
    if (superCats.isEmpty) {
      insertNode(uncatNode, node)
      model.nodeChanged(uncatNode)
    }
    
    entryAdded(food.code, superCats, node)
  }

  def categoryAdded(category: CategoryV2): Unit = {
    def node = createCategoryNode(category)
    
    if (categories.isRoot(category)) {
      insertNode(root, node)
      model.nodeChanged(root)
    }
    
    entryAdded(category.code, categories.categorySuperCategories(category.code), node)
  }

  def entryDeleted(code: String) = {
    nodes(code).foreach(node => {
      val parent = node.getParent()
      model.removeNodeFromParent(node)
      model.nodeChanged(parent)
    })

    nodes -= code
  }

  def createCategoryNode(category: CategoryV2): DefaultMutableTreeNode = {
    val catNode = new DefaultMutableTreeNode(CategoryWrapper(category))
    nodes += (category.code -> (catNode :: nodes(category.code)))

    category.subcategories.flatMap (categories.find(_)).sortBy(_.description).foreach (c => catNode.add(createCategoryNode(c)))
    category.foods.flatMap(foods.find(_)).sortBy(_.englishDescription).foreach (f => catNode.add(createFoodNode(f)))

    catNode
  }

  def selectedNode = Option(tree.getLastSelectedPathComponent()).map(_.asInstanceOf[DefaultMutableTreeNode].getUserObject().asInstanceOf[FoodTreeNode])

  def selectedPath = Option(tree.getSelectionPath()).map(_.getPath().map(_.asInstanceOf[DefaultMutableTreeNode].getUserObject().asInstanceOf[FoodTreeNode]))

  def selectedCategory = selectedPath.flatMap(_.reverse.find(_.isInstanceOf[CategoryWrapper]).map(_.asInstanceOf[CategoryWrapper].category.code))

  def selectedFood = selectedPath.flatMap((_.reverse.find(_.isInstanceOf[FoodWrapper]).map(_.asInstanceOf[FoodWrapper].food)))
  
  val uncatNode = new DefaultMutableTreeNode(Uncategorised)

  root.add(uncatNode)

  categories.uncategorisedFoods(foods.snapshot).sortBy(_.englishDescription).foreach(f => uncatNode.add(createFoodNode(f)))
  
  categories.rootCategories.foreach(cat => root.add(createCategoryNode(cat)))

  val checker = new ProblemChecker(foods, categories, portionSize)

  def check(node: FoodTreeNode) = node match {
    case RootNode => Seq()
    case FoodWrapper(food) => checker.problems(food)
    case CategoryWrapper(cat) => checker.problems(cat)
    case Uncategorised => if (uncatNode.getChildCount() > 0) Seq("These foods are not assigned to any category") else Seq()
  }

  val tree = new JTree(root)
  val model = tree.getModel().asInstanceOf[DefaultTreeModel]
  tree.setCellRenderer(new CellRenderer(check))
  tree.addTreeSelectionListener(new TreeSelectionListener {
    override def valueChanged(e: TreeSelectionEvent) =
      select(selectedNode)
  })
  tree.setSelectionModel(new DefaultTreeSelectionModel() {
    override def setSelectionPath(path: TreePath) = if (canChangeSelection()) super.setSelectionPath(path)
  })

  ToolTipManager.sharedInstance().registerComponent(tree)
  ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE)

  add(tree, BorderLayout.CENTER)
}