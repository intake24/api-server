/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import net.scran24.fooddef.Food

class MutableFoods (foods: Seq[Food]) {
  var map = foods.map ( f => (f.code, f)).toMap
  
  def find(code: String): Option[Food] = map.get(code)
  
  def create(food: Food) = 
    if (map.contains(food.code)) throw new DataException(s"Food with code ${food.code} already exists")
    else map = map + (food.code -> food)
    
  def delete(code: String) = map = map - code
  
  def update(code: String, food: Food) = map = map + (code -> food)
  
  def snapshot(): Seq[Food] = map.values.toSeq.sortBy(_.code)
  
  def tempcode() = {
      def mkCode (counter: Int) = "F%03d".format(counter)
      
      def rec(counter: Int): String = {
        if (counter == 999)
          throw new RuntimeException ("Someone has exceeded 999 temporary food entries... o_Oa")
        val code = mkCode(counter)
        if (find(code).isDefined)
          rec(counter + 1)
        else
          code
      }
      
      rec(0)
    }
}