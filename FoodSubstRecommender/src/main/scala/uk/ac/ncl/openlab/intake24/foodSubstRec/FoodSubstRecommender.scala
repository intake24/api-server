package uk.ac.ncl.openlab.intake24.foodSubstRec

import com.google.inject.Singleton
import javax.inject.Inject

/**
  * Created by Tim Osadchiy on 26/04/2018.
  */

case class MealFood(code: String, description: String, weightGrams: Option[Double])

case class RecommendedFood(original: Option[MealFood], options: Seq[(MealFood, Double)])

case class FoodDecision(givenFood: MealFood, alternatives: Seq[FoodDecision])

trait FoodSubstRecommender {

  def findAlternatives(foodCode: String): Seq[String]

}

@Singleton()
class FoodSubstRecommenderImpl @Inject()(nutrientDistanceService: FoodNutrientService,
                                         treeDistanceService: FoodTreeDistanceService) extends FoodSubstRecommender {

  private val MAX_TREE_DISTANCE = 5

  private case class FoodWithDistances(foodCode: String, nutrientDistance: Double, treeDistance: Double)

  override def findAlternatives(foodCode: String): Seq[String] = {

    val nutDistances = nutrientDistanceService.getClosest(foodCode)
    val treeDistances = treeDistanceService.getClosest(foodCode)
    val similarFoods = nutDistances.flatMap { nd =>
      for (
        treeDist <- treeDistances.get(nd._1);
        foodWithDistance = FoodWithDistances(nd._1, nd._2, treeDist)
        if nd._1 != foodCode
      ) yield foodWithDistance
    }.toSeq.filter { r => r.treeDistance <= MAX_TREE_DISTANCE }
      .sortBy(i => (i.treeDistance, i.nutrientDistance))
    similarFoods.map(_.foodCode)
  }

}
