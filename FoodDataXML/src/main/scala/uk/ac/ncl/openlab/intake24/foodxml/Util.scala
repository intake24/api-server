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
*/

package uk.ac.ncl.openlab.intake24.foodxml

import scala.xml.Node
import scala.xml.PrettyPrinter
import java.io.PrintWriter
import java.io.File
import net.scran24.fooddef.CategoryV1
import net.scran24.fooddef.FoodOld
import net.scran24.fooddef.IndexEntryOld

object Util {
  def mapFoods (rootCats: Map[String, CategoryV1], f: FoodOld => FoodOld) : Map[String, CategoryV1] = {
    def mapRec (root: Map[String, IndexEntryOld]) : Map[String, IndexEntryOld] = {
      root.mapValues {
        case ef: FoodOld => f(ef)
        case ec: CategoryV1 => ec.copy (children = mapRec (ec.children))        
      }
    }
    rootCats.mapValues( ec => ec.copy (children = mapRec(ec.children)))
  }
  
  def flatten[T] (rootCats: Map[String, CategoryV1], f: FoodOld => T) : Seq[T] = {
    def mapRec (root: Map[String, IndexEntryOld]) : List[T] = {
      root.values.foldLeft(List[T]())( (list, next) => next match {
        case ef: FoodOld => f(ef) :: list
        case ec: CategoryV1 => mapRec(ec.children) ::: list
      })
    }
    
    mapRec(rootCats)
  }
  
  def writeXml(root: Node, path: String) = {
    val pp = new PrettyPrinter(180, 2)
    
    val writer = new PrintWriter(new File(path))
    writer.println("<?xml version='1.0' encoding='utf-8'?>")
    writer.println(pp.format(root))
    writer.close
  }
}