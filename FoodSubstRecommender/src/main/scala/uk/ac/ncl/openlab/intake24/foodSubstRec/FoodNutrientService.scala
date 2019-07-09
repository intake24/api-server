package uk.ac.ncl.openlab.intake24.foodSubstRec

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.algs.StandardScaler
import uk.ac.ncl.openlab.intake24.services.nutrition.FoodCompositionService

/**
  * Created by Tim Osadchiy on 25/04/2018.
  */
trait FoodNutrientService extends DistanceService {
  def getNutrients(foodCode: String): Option[Map[Long, Double]]
}

@Singleton()
class FoodNutrientServiceEnGbNDNSImpl @Inject()(val nutrientService: FoodCompositionService) extends FoodNutrientService {

  // Fixme: Top priority. Make all calculations async in a separate thread

  // Fixme: This service should work for any locale and tableId

  private val logger = LoggerFactory.getLogger(getClass)

  // Fixme: Cached needs to be refreshed for newly added foods
  private val foodNutrientMap: Map[String, Map[Long, Double]] =
    nutrientService.listFoodNutrients("NDNS", "en_GB_v1") match {
      case Right(r) => r
      case Left(e) =>
        logger.error(s"Couldn't get food nutrients. ${e.exception.getMessage}")
        Map().withDefaultValue(Map().withDefaultValue(Double.MaxValue))
    }

  private val nutrientTypes: Seq[Long] = foodNutrientMap.foldLeft(foodNutrientMap.head._2.keys.toSet) {
    (agg, foodNuts) => agg.intersect(foodNuts._2.keys.toSet)
  }.toSeq.sorted

  private val scaled: Map[String, Map[Long, Double]] = {

    val data = foodNutrientMap.map { nm =>
      nutrientTypes.map(nt => nm._2(nt)).toVector
    }.toVector

    val scalerModel = StandardScaler(withMean = false).fit(data)
    val transformed = scalerModel.transform(data)

    foodNutrientMap.zipWithIndex.map { foodNode =>
      foodNode._1._1 -> nutrientTypes.zipWithIndex.map { nti =>
        nti._1 -> transformed(foodNode._2)(nti._2)
      }.toMap
    }

  }

  override def getClosest(foodCode: String): Map[String, Double] = scaled.get(foodCode).map { foodNuts1 =>
    scaled.collect {
      case food if food._1 != foodCode => food._1 -> {
        val foodNuts2 = food._2
        val sqrSum = foodNuts1.foldLeft(0d) { (agg, foodNut1) => agg + Math.pow(foodNut1._2 - foodNuts2(foodNut1._1), 2) }
        Math.sqrt(sqrSum)
      }
    }
  }.getOrElse(Map())

  override def getNutrients(foodCode: String): Option[Map[Long, Double]] = foodNutrientMap.get(foodCode)

}
