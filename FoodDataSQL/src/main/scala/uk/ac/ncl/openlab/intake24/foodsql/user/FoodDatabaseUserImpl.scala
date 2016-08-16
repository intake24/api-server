package uk.ac.ncl.openlab.intake24.foodsql.user

import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodDatabaseService
import javax.sql.DataSource
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named

@Singleton   
class FoodDatabaseUserImpl @Inject() (@Named("intake24_foods") val dataSource: DataSource) extends FoodDatabaseService
  with AsServedImageUserImpl
  with GuideImageUserImpl
  with DrinkwareUserImpl
  with AssociatedFoodsUserImpl
  with BrandNamesUserImpl  
  with FoodBrowsingUserImpl
  with FoodDataUserImpl { }
