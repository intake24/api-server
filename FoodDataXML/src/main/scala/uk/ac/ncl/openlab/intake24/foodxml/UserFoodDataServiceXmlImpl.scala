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

package uk.ac.ncl.openlab.intake24.foodxml

import com.google.inject.{Inject, Singleton}
import org.slf4j.LoggerFactory
import uk.ac.ncl.openlab.intake24._
import uk.ac.ncl.openlab.intake24.errors.{LocaleError, LookupError, RecordNotFound}
import uk.ac.ncl.openlab.intake24.services.fooddb.user._

@Singleton
class UserFoodDataServiceXmlImpl @Inject() (data: XmlDataSource) extends FoodDatabaseService {

  import Util._

  val log = LoggerFactory.getLogger(classOf[UserFoodDataServiceXmlImpl])

  def getRootCategories(locale: String): Either[LocaleError, Seq[UserCategoryHeader]] =
    checkLocale(locale).right.map(_ => data.categories.rootCategories.map(mkHeader))

  def getCategoryContents(code: String, locale: String) =
    checkLocale(locale).right.flatMap {
      _ =>
        data.categories.categoryMap.get(code) match {
          case Some(cat) => {
            val foodHeaders = cat.foods.map(fcode => mkHeader(data.foods.find(fcode)))
            val categoryHeaders = cat.subcategories.map(catcode => mkHeader(data.categories.find(catcode)))

            Right(UserCategoryContents(foodHeaders, categoryHeaders))

          }
          case None => Left(RecordNotFound(new RuntimeException(s"code: $code, locale: $locale")))
        }
    }

  def getFoodAllCategories(code: String) = Right(data.categories.foodAllCategories(code).toSet)

  def getCategoryAllCategories(code: String) = Right(data.categories.categoryAllCategories(code).toSet)

  def isCategoryCode(code: String) = data.categories.categoryMap.contains(code)

  def getFoodData(code: String, locale: String) =
    checkLocale(locale).right.map {
      _ =>

        val f = data.foods.find(code)

        val portionSizeMethods = {
          val ps = f.portionSizeMethods
          if (ps.isEmpty) {
            data.inheritance.foodInheritedPortionSize(code)
          } else
            ps
        }

        val readyMealOption = f.attributes.readyMealOption match {
          case Some(value) => value
          case None => data.inheritance.foodInheritedAttribute(code, _.readyMealOption) match {
            case Some(value) => value
            case None => InheritableAttributes.readyMealDefault
          }
        }

        val sameAsBeforeOption = f.attributes.sameAsBeforeOption match {
          case Some(value) => value
          case None => data.inheritance.foodInheritedAttribute(code, _.sameAsBeforeOption) match {
            case Some(value) => value
            case None => InheritableAttributes.sameAsBeforeDefault
          }
        }

        val reasonableAmount = f.attributes.reasonableAmount match {
          case Some(value) => value
          case None => data.inheritance.foodInheritedAttribute(code, _.reasonableAmount) match {
            case Some(value) => value
            case None => InheritableAttributes.reasonableAmountDefault
          }
        }

        ??? /// (UserFoodData(f.code, f.description, f.nutrientTableCodes, f.groupCode, portionSizeMethods, readyMealOption, sameAsBeforeOption, reasonableAmount), null)
    }

  def getAsServedSet(id: String) = data.asServedSets.get(id) match {
    case Some(set) => Right(UserAsServedSet(set.images(0).url, set.images.map( m => UserAsServedImage(m.url, m.url, m.weight))))
    case None => Left(RecordNotFound(new RuntimeException(id)))
  }

  def getGuideImage(id: String) = data.guideImages.get(id) match {
    case Some(image) => ??? // Right(image)
    case None => Left(RecordNotFound(new RuntimeException(id)))
  }

  def getDrinkwareSet(id: String) = data.drinkwareSets.get(id) match {
    case Some(set) => Right(set)
    case None => Left(RecordNotFound(new RuntimeException(id)))
  }

  def getAssociatedFoods(foodCode: String, locale: String) =
    checkLocale(locale).right.flatMap {
      _ =>
        data.prompts.get(foodCode) match {
          case Some(seq) => {
            Right(seq.map {
              v1 =>
                data.categories.categoryMap.get(v1.category) match {
                  case Some(category) => AssociatedFood(Right(category.code), v1.promptText, v1.linkAsMain, v1.genericName)
                  case None => {
                    val food = data.foods.find(v1.category)
                    AssociatedFood(Left(food.code), v1.promptText, v1.linkAsMain, v1.genericName)
                  }
                }
            })
          }
          case None => Right(Seq())
        }
    }

  def getBrandNames(foodCode: String, locale: String) =
    checkLocale(locale).right.flatMap {
      _ =>
        data.brandNamesMap.get(foodCode) match {
          case Some(map) => Right(map)
          case None => Right(Seq())
        }
    }

  override def getImageMap(id: String): Either[LookupError, UserImageMap] = ???

  override def getImageMaps(id: Seq[String]): Either[LookupError, Seq[UserImageMap]] = ???
}
