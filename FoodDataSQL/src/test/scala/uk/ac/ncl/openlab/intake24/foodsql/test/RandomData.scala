package uk.ac.ncl.openlab.intake24.foodsql.test

import uk.ac.ncl.openlab.intake24._

import scala.util.Random

trait RandomData {

  def randomString(length: Int) = Random.alphanumeric.take(length).mkString

  val undefinedCode = "bad_code"

  def randomCount(min: Int, max: Int) = {
    def bound = max - min
    if (bound <= 0)
      max
    else
      Random.nextInt(bound) + min
  }

  def randomCode(): String = {
    val t = randomString(Random.nextInt(3) + 4)
    if (t == undefinedCode)
      randomCode()
    else
      t
  }

  def randomUniqueId[T](existing: Set[T], nextRandomId: => T): T = {
    val t = nextRandomId
    if (existing.contains(t))
      randomUniqueId(existing, nextRandomId)
    else
      t
  }

  def randomUniqueIds[T](n: Int, nextRandomId: => T): Set[T] = {

    def rec(remaining: Int, existing: Set[T]): Set[T] = {
      if (remaining == 0)
        existing
      else
        rec(remaining - 1, existing + randomUniqueId(existing, nextRandomId))
    }

    rec(n, Set())
  }

  def randomElement[T](seq: IndexedSeq[T]) = seq(Random.nextInt(seq.size))

  def randomDescription = randomString(Random.nextInt(63) + 1)

  def randomIdentifier = randomString(Random.nextInt(32) + 1)

  def randomAttributes = {
    val readyMeal = if (Random.nextBoolean()) Some(Random.nextBoolean()) else None
    val sameAsBefore = if (Random.nextBoolean()) Some(Random.nextBoolean()) else None
    val reasonableAmount = if (Random.nextBoolean()) Some(Random.nextInt(1000)) else None

    InheritableAttributes(readyMeal, sameAsBefore, reasonableAmount)
  }

  def randomNewFood(code: String, groupCodes: IndexedSeq[Int]) = NewMainFoodRecord(code, randomDescription, randomElement(groupCodes), randomAttributes, Seq(), Seq())

  def randomNewFoods(min: Int, max: Int, groupCodes: IndexedSeq[Int]) = {
    val count = randomCount(min, max)

    val codes = randomUniqueIds(count, randomCode).toIndexedSeq

    codes.map(randomNewFood(_, groupCodes))
  }

  def randomNewCategory(code: String) = NewCategory(code, randomDescription, Random.nextBoolean(), randomAttributes)

  def randomNewCategories(min: Int, max: Int) = {
    val count = randomCount(min, max)

    val codes = randomUniqueIds(count, randomCode).toIndexedSeq

    codes.map(randomNewCategory(_))
  }

  def randomLocalFoods(forFoods: Seq[String], assocFoods: IndexedSeq[String], assocCategories: IndexedSeq[String], nutrientTableRecords: IndexedSeq[NewNutrientTableRecord]) = {
    forFoods.foldLeft(Map[String, NewLocalFoodRecord]()) {
      (map, code) => map + (code -> randomLocalFoodRecord(nutrientTableRecords, assocFoods, assocCategories))
    }
  }

  def randomNutrientTables(min: Int, max: Int) = {
    val count = randomCount(min, max)

    IndexedSeq.fill(count) {
      NutrientTable(randomIdentifier, randomDescription)
    }
  }

  def randomNutrientTableRecords(forTables: IndexedSeq[NutrientTable], min: Int, max: Int) = {
    forTables.flatMap {
      table =>
        val count = randomCount(min, max)
        val q = Seq(1l, 2l, 3l, 4l, 5l)
        Seq.fill(count) {
          NewNutrientTableRecord(table.id, randomIdentifier, randomDescription, None, q.map(n => (n -> Random.nextDouble() * 100.0)).toMap)
        }
    }
  }

  def randomNutrientCodes(nutrientTableRecords: IndexedSeq[NewNutrientTableRecord]) =
    nutrientTableRecords.groupBy(_.nutrientTableId).foldLeft(Map[String, String]()) {
      case (result, (tableCode, records)) =>
        if (Random.nextBoolean())
          result + (tableCode -> randomElement(records).id)
        else
          result
    }

  def randomLocalFoodRecord(nutrientTableRecords: IndexedSeq[NewNutrientTableRecord], assocFoodCodes: IndexedSeq[String], assocCategoryCodes: IndexedSeq[String]) =
    NewLocalFoodRecord(Some(randomDescription), Random.nextBoolean(), randomNutrientCodes(nutrientTableRecords), randomPortionSizeMethods,
      randomAssociatedFoods(assocFoodCodes, assocCategoryCodes), randomBrands)

  def randomPortionSizeMethod = {

    def randomParameters = {
      val count = Random.nextInt(5)

      Seq.fill(count)(PortionSizeMethodParameter(randomIdentifier, randomDescription))
    }

    PortionSizeMethod(randomIdentifier, randomDescription, randomDescription, Random.nextBoolean(), randomParameters)
  }

  def randomPortionSizeMethods = {
    val count = Random.nextInt(5)

    Seq.fill(count)(randomPortionSizeMethod)
  }

  def randomLocalCategoryRecord =
    NewLocalCategoryRecord(Some(randomDescription), randomPortionSizeMethods)

  def randomLocalCategoryRecords(forCategories: Seq[String]) = {
    forCategories.foldLeft(Map[String, NewLocalCategoryRecord]()) {
      (map, code) =>
        map + (code -> randomLocalCategoryRecord)
    }
  }

  def randomFoodGroup(id: Int) = FoodGroupMain(id, randomDescription)

  def randomFoodGroups(min: Int, max: Int) = {
    val count = randomCount(min, max)

    val ids = randomUniqueIds(count, Random.nextInt()).toIndexedSeq

    ids.map(randomFoodGroup(_))
  }

  def randomLocalFoodGroups(forGroups: Seq[FoodGroupMain]) = forGroups.foldLeft(Map[Int, FoodGroupLocal]()) {
    (map, group) =>
      map + (group.id -> FoodGroupLocal(Some(randomDescription)))
  }

  def randomAsServedImage = AsServedImageV1(randomIdentifier, Random.nextDouble() * 100)

  def randomAsServedSet = {
    val count = Random.nextInt(5)

    AsServedSetV1(randomIdentifier, randomDescription, Seq.fill(count)(randomAsServedImage))
  }

  def randomAsServedSets = {
    val count = Random.nextInt(4) + 1

    IndexedSeq.fill(count)(randomAsServedSet)
  }

  def randomAssociatedFood(assocFoodCodes: IndexedSeq[String], assocCategoryCodes: IndexedSeq[String]) = {
    val foodOrCategory = (assocFoodCodes.nonEmpty, assocCategoryCodes.nonEmpty) match {
      case (true, true) =>
        {
          if (Random.nextBoolean())
            Left(randomElement(assocFoodCodes))
          else
            Right(randomElement(assocCategoryCodes))

        }
      case (true, false) => Left(randomElement(assocFoodCodes))
      case (false, true) => Right(randomElement(assocCategoryCodes))
      case (false, false) => throw new RuntimeException("both assocFoodCodes and assocCategoryCodes cannot be empty")
    }

    AssociatedFood(foodOrCategory, randomDescription, Random.nextBoolean(), randomDescription)
  }

  def randomAssociatedFoods(assocFoodCodes: IndexedSeq[String], assocCategoryCodes: IndexedSeq[String]) =
    {
      if (assocFoodCodes.nonEmpty || assocCategoryCodes.nonEmpty) {
        val count = Random.nextInt(5)
        IndexedSeq.fill(count)(randomAssociatedFood(assocFoodCodes, assocCategoryCodes))
      } else IndexedSeq()
    }

  def randomAssociatedFoodsFor(codes: Seq[String], assocFoodCodes: IndexedSeq[String], assocCategoryCodes: IndexedSeq[String]) =
    {
      codes.map(_ -> randomAssociatedFoods(assocFoodCodes, assocCategoryCodes)).toMap
    }

  def randomBrands =
    {
      val count = Random.nextInt(5)
      Seq.fill(count)(randomDescription)
    }

  def randomBrandsFor(codes: Seq[String]) = codes.map(_ -> randomBrands).toMap
}