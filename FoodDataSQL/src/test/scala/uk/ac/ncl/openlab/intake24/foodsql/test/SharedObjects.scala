package uk.ac.ncl.openlab.intake24.foodsql.test

import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.NewFood
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter

object SharedObjects {
  val testLocale = Locale("locale1", "Locale 1", "Великая локаль 1", "en", "en", "gb", None)  
  val testFoodGroups = Seq(FoodGroupMain(1, "Test food group"))
  
  val testFood1 = NewFood("TF01", "Test food 1", 1, InheritableAttributes(None, None, None))
  val testFood2 = NewFood("TF02", "Test food 2", 1, InheritableAttributes(None, None, None))
  
  val testCat1 = NewCategory("CAT01", "Test category 1", false, InheritableAttributes(None, None, None))
  val testCat2 = NewCategory("CAT02", "Test category 2", false, InheritableAttributes(None, None, None))

  val testPsm1 = PortionSizeMethod("method 1", "description 1", "image1.jpg", false, Seq(PortionSizeMethodParameter("param1", "1"), PortionSizeMethodParameter("param2", "2")))
  val testPsm2 = PortionSizeMethod("method 2", "description 2", "image2.jpg", true, Seq())
}