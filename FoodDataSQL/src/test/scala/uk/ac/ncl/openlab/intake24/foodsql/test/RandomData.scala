package uk.ac.ncl.openlab.intake24.foodsql.test

import scala.util.Random
import uk.ac.ncl.openlab.intake24.MainFoodRecord
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.NewFood
import uk.ac.ncl.openlab.intake24.AsServedImage
import uk.ac.ncl.openlab.intake24.AsServedSet
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import uk.ac.ncl.openlab.intake24.PortionSizeMethodParameter
import uk.ac.ncl.openlab.intake24.PortionSizeMethod
import uk.ac.ncl.openlab.intake24.FoodGroupLocal

trait RandomData {

  def randomString(length: Int) = Random.alphanumeric.take(length).mkString

  val undefinedCode = "bad_code"

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

  def randomNewFood(code: String, groupCodes: IndexedSeq[Int]) = NewFood(code, randomDescription, randomElement(groupCodes), randomAttributes)

  def randomNewFoods(min: Int, max: Int, groupCodes: IndexedSeq[Int]) = {
    val count = Random.nextInt(max - min) + min

    val codes = randomUniqueIds(count, randomCode).toIndexedSeq

    codes.map(randomNewFood(_, groupCodes))
  }

  def randomNewCategory(code: String) = NewCategory(code, randomDescription, Random.nextBoolean(), randomAttributes)

  def randomNewCategories(min: Int, max: Int) = {
    val count = Random.nextInt(max - min) + min

    val codes = randomUniqueIds(count, randomCode).toIndexedSeq

    codes.map(randomNewCategory(_))
  }

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
    LocalCategoryRecord(None, Some(randomDescription), randomPortionSizeMethods)

  def randomLocalCategoryRecords(forCategories: Seq[String]) = {
    forCategories.foldLeft(Map[String, LocalCategoryRecord]()) {
      (map, code) =>
        map + (code -> randomLocalCategoryRecord)
    }
  }

  def randomFoodGroup(id: Int) = FoodGroupMain(id, randomDescription)

  def randomFoodGroups(min: Int, max: Int) = {
    val count = Random.nextInt(max - min) + min

    val ids = randomUniqueIds(count, Random.nextInt()).toIndexedSeq

    ids.map(randomFoodGroup(_))
  }
  
  def randomLocalFoodGroups(forGroups: Seq[FoodGroupMain]) = forGroups.foldLeft(Map[Int, FoodGroupLocal]()) {
    (map, group) =>
      map + (group.id -> FoodGroupLocal(Some(randomDescription))) 
  }

  def randomAsServedImage = AsServedImage(randomIdentifier, Random.nextDouble() * 100)

  def randomAsServedSet = {
    val count = Random.nextInt(5)

    AsServedSet(randomIdentifier, randomDescription, Seq.fill(count)(randomAsServedImage))
  }

  def randomAsServedSets = {
    val count = Random.nextInt(4) + 1

    IndexedSeq.fill(count)(randomAsServedSet)
  }

  def randomAssociatedFood(assocFoodCodes: IndexedSeq[String], assocCategoryCodes: IndexedSeq[String]) = {

    val foodOrCategory = if (Random.nextBoolean())
      Left(randomElement(assocFoodCodes))
    else
      Right(randomElement(assocCategoryCodes))

    AssociatedFood(foodOrCategory, randomDescription, Random.nextBoolean(), randomDescription)
  }

  def randomAssociatedFoods(forCodes: Seq[String], assocFoodCodes: IndexedSeq[String], assocCategoryCodes: IndexedSeq[String]) = {
    forCodes.foldLeft(Map[String, Seq[AssociatedFood]]()) {
      (map, code) =>
        {

          val count = Random.nextInt(5)
          map + (code -> Seq.fill(count)(randomAssociatedFood(assocFoodCodes, assocCategoryCodes)))
        }
    }
  }

  def randomBrands(forCodes: Seq[String]) = forCodes.foldLeft(Map[String, Seq[String]]()) {
    (map, code) =>
      {
        val count = Random.nextInt(5)
        map + (code -> Seq.fill(count)(randomDescription))
      }
  }
}