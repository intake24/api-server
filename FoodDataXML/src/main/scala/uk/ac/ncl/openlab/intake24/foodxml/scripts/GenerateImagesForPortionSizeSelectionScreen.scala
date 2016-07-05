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

package uk.ac.ncl.openlab.intake24.foodxml.scripts

import scala.xml.XML
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import scala.sys.process._
import uk.ac.ncl.openlab.intake24.DrinkScale
import uk.ac.ncl.openlab.intake24.foodxml.FoodDef
import uk.ac.ncl.openlab.intake24.foodxml.GuideImageDef
import uk.ac.ncl.openlab.intake24.foodxml.DrinkwareDef
import uk.ac.ncl.openlab.intake24.foodxml.AsServedDef
import uk.ac.ncl.openlab.intake24.foodxml.Util

object GenerateImagesForPortionSizeSelectionScreen extends App {
  
  val foodsPath = "/home/ivan/Projects/Intake24/intake24-data/foods.xml"
  val foodsOutputPath = "/home/ivan/Projects/Intake24/intake24-data/foods-modified.xml"
  
  val asServedPath = "/home/ivan/Projects/Intake24/intake24-data/as-served.xml"
  val guidePath = "/home/ivan/Projects/Intake24/intake24-data/guide.xml"
  val drinkScalePath = "/home/ivan/Projects/Intake24/intake24-data/drinkware.xml"
  
  val imageBase = "/home/ivan/Projects/Intake24/intake24-images/"
  val imageDest = "/home/ivan/tmp/imagetest/"
  
  val foods = FoodDef.parseXml(XML.load(foodsPath))
  val asServed = AsServedDef.parseXml(XML.load(asServedPath))
  val guide = GuideImageDef.parseXml(XML.load(guidePath))
  val drinkware = DrinkwareDef.parseXml(XML.load(drinkScalePath))
 
  def makeDescription(method: PortionSizeMethod) = {
    method.method match {
      case "as-served" => "Use an image" 
      case "guide-image" => "Use an image"
      case "drink-scale" => "Use an image"
      case "standard-portion" => "Choose a standard portion"
      case "cereal" => "Use an image"
      case "milk-in-a-hot-drink" => "Choose a standard portion"
      case "milk-on-cereal" => "Use an image"
      case "pizza" => "Use an image"
    }
  }

  def resize(relativeSourcePath: String, relativeDestPath: String) = {
    val command = "convert %s -resize 300x200 %s".format(imageBase + relativeSourcePath, imageDest + relativeDestPath)

    command.! match {
      case 0 => relativeDestPath
      case _@code => throw new RuntimeException("ImageMagick command (%s) failed with code %d".format(command, code))
    }
  }
  
  def generateAnImage(method: PortionSizeMethod): String = {
    
    val params = method.parameters.map( p => (p.name, p.value)).toMap
    
    method.method match {
      case "as-served" => {
        val set = asServed(params("serving-image-set"))        
        val imageIndex = set.images.length / 2                
        
        resize(set.images(imageIndex).url, set.id + ".jpg")        
      }
      
      case "guide-image" => {
        val guideImage = guide(params("guide-image-id"))
        val relativeSrcPath = guideImage.id + ".jpg"
        val relativeDestPath = guideImage.id + ".jpg"
        
        resize (relativeSrcPath, relativeDestPath)
      }
      
      case "drink-scale" => {
        val drinkScale = drinkware(params("drinkware-id"))
        
        val relativeSrcPath = drinkScale.guide_id + ".jpg"
        val relativeDestPath = drinkScale.guide_id + ".jpg"
        
        resize (relativeSrcPath, relativeDestPath)        
      }
      
      case "standard-portion" => "standard-portion.jpg"
        
      case "cereal" => resize ("cereal/flakeA4.jpg", "cereal.jpg")
      
      case "milk-in-a-hot-drink" => "standard-portion.jpg"
      
      case "milk-on-cereal" => resize ("cereal/milkbowlA.jpg", "milk_on_cereal.jpg")
      
      case "pizza" => resize ("gpiz2.jpg", "pizza.jpg")
        
      case _ => throw new RuntimeException("Unexpected portion size method name: " + method.method)      
    }        
  }
  
  def fix(code: String, method: PortionSizeMethod) = {
    println ("Processing " + code + " " + method.method)
    
    val fixDesc = method.description.toLowerCase() == "no description"
    val fixImage = method.imageUrl == "portion/placeholder.jpg"
    
    val desc = if (method.description.toLowerCase() == "no description") makeDescription(method) else method.description
    val imageUrl = if (method.imageUrl == "portion/placeholder.jpg") "portion/" + generateAnImage(method) else method.imageUrl
    
    if (fixDesc) println ("  Fixed description for " + code)
    if (fixImage) println ("  Fixed image for " + code)
    
    method.copy(description = desc, imageUrl = imageUrl)        
  }
  
  val fixedFoods = foods.map {
    food => 
        val fixedPortionSize = food.localData.portionSize.map(m => fix(food.code, m))
        food.copy (localData = food.localData.copy(portionSize = fixedPortionSize))
  }
  
  Util.writeXml(FoodDef.toXml(fixedFoods), foodsOutputPath)
}