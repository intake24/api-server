package models

import uk.ac.ncl.openlab.intake24.FoodRecord
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.LocalFoodRecord
import uk.ac.ncl.openlab.intake24.UserAssociatedFood

case class AdminFoodRecord(main: MainFoodRecord, local: LocalFoodRecord, brands: Seq[String], associatedFoods: Seq[UserAssociatedFood])  
