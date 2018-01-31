package models

import uk.ac.ncl.openlab.intake24.api.data.admin.{AssociatedFoodWithHeader, LocalFoodRecord, MainFoodRecord}

case class AdminFoodRecord(main: MainFoodRecord, local: LocalFoodRecord, brands: Seq[String], associatedFoods: Seq[AssociatedFoodWithHeader])
