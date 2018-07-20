package uk.ac.ncl.openlab.intake24.foodSubstRec

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24.services.fooddb.user.FoodBrowsingService

/**
  * Created by Tim Osadchiy on 26/04/2018.
  */
trait FoodRepoCacheService {

  def getFoodCategories(foodCode: String, level: Int): Seq[String]

  def getAllFoodsFromCategory(category: String): Seq[String]

  def getCategoryCategories(subcategoryCode: String): Seq[String]

}

@Singleton()
class FoodRepoCacheServiceImpl @Inject()(foodService: FoodBrowsingService) extends FoodRepoCacheService {

  private val logger = LoggerFactory.getLogger(getClass)

  private val foodCategoryRecords = foodService.listFodCategoryRelationships().getOrElse(Seq())

  private val foodCategoryRelationships: Map[String, Seq[String]] = foodService.listFodCategoryRelationships() match {
    case Right(r) => r.groupBy(_.foodCode).map(i => i._1 -> i._2.map(_.categoryCode))
    case Left(e) =>
      logger.error(s"Couldn't list food categories. ${e.exception.getMessage}")
      Map()
  }

  private val categoryCategoryRelationships: Map[String, Seq[String]] = foodService.listCategoryCategoryRelationships() match {
    case Right(ccr) => ccr.groupBy(_.categoryCode).map(i => i._1 -> i._2.map(_.subCategoryCode))
    case Left(e) =>
      logger.error(s"Couldn't list categories relationships. ${e.exception.getMessage}")
      Map()
  }

  private val categoryAllFoodRelationships: Map[String, Seq[String]] = {
    /** Map of all foods nested in a category **/
    val mp = foodCategoryRecords
      .groupBy(_.categoryCode).map(n => n._1 -> n._2.map(_.foodCode)).withDefaultValue(Seq())
    mp ++ categoryCategoryRelationships.map(kv => kv._1 -> {
      val categories = kv._2 ++ kv._2.flatMap(getSubcategories)
      categories.flatMap(c => mp(c))
    }).withDefaultValue(Seq())
  }

  private def getSubcategories(categoryCode: String): Seq[String] = {
    def recursion(categoryCodes: Seq[String]): Seq[String] = categoryCodes.flatMap { code =>
      categoryCategoryRelationships.get(code)
    }.flatten.distinct.filterNot(categoryCodes.contains(_)) match {
      case Nil => Nil
      case l => l ++ recursion(l)
    }

    recursion(Seq(categoryCode))
  }

  override def getFoodCategories(foodCode: String, level: Int): Seq[String] = {
    var c = 0
    var categories = foodCategoryRelationships.getOrElse(foodCode, Seq())
    while (c < level) {
      categories = categories.flatMap { cat =>
        getCategoryCategories(cat) match {
          case Nil => Seq(cat)
          case l => l
        }
      }
      c += 1
    }
    categories
  }

  override def getAllFoodsFromCategory(category: String): Seq[String] = categoryAllFoodRelationships(category)

  override def getCategoryCategories(subcategoryCode: String): Seq[String] =
    categoryCategoryRelationships.filter(cat => cat._2.contains(subcategoryCode)).keys.toSeq

}
