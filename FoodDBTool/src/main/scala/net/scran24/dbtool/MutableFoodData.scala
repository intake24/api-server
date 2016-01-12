/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import net.scran24.fooddef.Category
import net.scran24.fooddef.Food

trait FoodDataListener {
  def dataChanged      
}

class MutableFoodData {
  def rootCategories(): Seq[Category] = ???
  def foodByCode(code: String): Food = ???
  def categoryByCode(code: String): Food = ???
  
  def addListener(listener: FoodDataListener) = ???  
}