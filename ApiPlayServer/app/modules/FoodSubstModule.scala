package modules

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import uk.ac.ncl.openlab.intake24.foodSubstRec._

/**
  * Created by Tim Osadchiy on 26/04/2018.
  */
class FoodSubstModule(env: Environment, config: Configuration) extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[FoodNutrientService]).to(classOf[FoodNutrientServiceEnGbNDNSImpl])
    bind(classOf[FoodRepoCacheService]).to(classOf[FoodRepoCacheServiceImpl])
    bind(classOf[FoodSubstApi]).to(classOf[FoodSubstApiImpl])
    bind(classOf[FoodSubstRecommender]).to(classOf[FoodSubstRecommenderImpl])
    bind(classOf[FoodTreeDistanceService]).to(classOf[FoodTreeDistanceServiceImpl])

  }

}
