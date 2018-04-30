package uk.ac.ncl.openlab.intake24.foodSubstRec

import com.google.inject.{Inject, Singleton}

/**
  * Created by Tim Osadchiy on 26/04/2018.
  */

trait FoodTreeDistanceService extends DistanceService

@Singleton()
class FoodTreeDistanceServiceImpl @Inject()(foodCacheService: FoodRepoCacheService) extends FoodTreeDistanceService {

  override def getClosest(foodCode: String): Map[String, Double] = {
    val foodCategories = foodCacheService.getFoodCategories(foodCode, 0)

    def recursion(categoryCodes: Seq[String], level: Int): Seq[(String, Double)] =
      categoryCodes.flatMap(c => foodCacheService.getAllFoodsFromCategory(c).map(f => f -> level.toDouble)) ++
        (categoryCodes.flatMap { code =>
          foodCacheService.getCategoryCategories(code)
        }.filterNot(categoryCodes.contains) match {
          case Nil => Nil
          case l => recursion(l, level + 1)
        })

    recursion(foodCategories, 0).filterNot(i => i._1 == foodCode)
      .foldLeft(Map[String, Double]()) { (agg, item) =>
        agg + (item._1 -> {
          if (agg.getOrElse(item._1, Double.MaxValue) > item._2) {
            item._2
          } else {
            agg(item._1)
          }
        })
      }
  }

}
