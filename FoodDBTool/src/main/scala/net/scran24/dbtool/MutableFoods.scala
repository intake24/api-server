/*
This file is part of Intake24.

© Crown copyright, 2012, 2013, 2014.

This software is licensed under the Open Government Licence 3.0:

http://www.nationalarchives.gov.uk/doc/open-government-licence/
*/

package net.scran24.dbtool

import uk.ac.ncl.openlab.intake24.FoodRecord

class MutableFoods (foods: Seq[FoodRecord]) {
  var map = foods.map ( f => (f.main.code, f)).toMap
  
  def find(code: String): Option[FoodRecord] = map.get(code)
  
  def create(food: FoodRecord) = 
    if (map.contains(food.main.code)) throw new DataException(s"Food with code ${food.main.code} already exists")
    else map = map + (food.main.code -> food)
    
  def delete(code: String) = map = map - code
  
  def update(code: String, food: FoodRecord) = map = map + (code -> food)
  
  def snapshot(): Seq[FoodRecord] = map.values.toSeq.sortBy(_.main.code)
  
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