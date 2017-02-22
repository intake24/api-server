package apidocs

import uk.ac.ncl.openlab.intake24.{CategoryHeader, FoodHeader, UserCategoryHeader, UserFoodHeader}

object IntroExamples {

  import JSONPrettyPrinter._

  private case class ObjWithOptString(value: Option[String])

  private case class ObjWithNestedOption(value: Option[Option[String]])

  val optionalStringPresent = asPrettyJSON(ObjWithOptString(Some("Hello")))
  val optionalStringMissing = asPrettyJSON(ObjWithOptString(None))

  val nestedOptionMissingInner = asPrettyJSON(ObjWithNestedOption(Some(None)))

  val ef: Either[FoodHeader, CategoryHeader] = Left(FoodHeader("FOOD001", "Example food", Some("Пример продукта"), false))
  val ec: Either[FoodHeader, CategoryHeader] = Right(CategoryHeader("CAT001", "Example category", None, false))

  private case class ObjWithEither(foodOrCategory: Either[FoodHeader, CategoryHeader])

  val eitherFood = asPrettyJSON(ObjWithEither(ef))
  val eitherCategory = asPrettyJSON(ObjWithEither(ec))

  private case class ObjWithEither2(optionalLeftOrRight: Either[Option[String], String])

  val eitherOption = asPrettyJSON(ObjWithEither2(Left(None)))
}
