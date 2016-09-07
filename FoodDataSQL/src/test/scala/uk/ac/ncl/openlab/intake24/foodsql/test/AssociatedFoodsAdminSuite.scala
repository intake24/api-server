package uk.ac.ncl.openlab.intake24.foodsql.test

import org.scalatest.FunSuite
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import org.scalatest.DoNotDiscover
import uk.ac.ncl.openlab.intake24.AsServedImage
import uk.ac.ncl.openlab.intake24.AsServedSet

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import org.scalatest.BeforeAndAfterAll
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.NewFood
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentRecordNotFound

@DoNotDiscover
class AssociatedFoodsAdminSuite(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll with FixedData with RandomData {

  /*
   *   def associatedFoods(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFoodWithHeader]]
  def updateAssociatedFoods(foodCode: String, locale: String, associatedFoods: Seq[AssociatedFood]): Either[LocalLookupError, Unit]
  
  def deleteAllAssociatedFoods(locale: String): Either[DatabaseError, Unit]
  def createAssociatedFoods(assocFoods: Map[String, Seq[AssociatedFood]], locale: String): Either[DatabaseError, Unit]
   */

  val foodGroups = randomFoodGroups(2, 10)

  val referenceFoods = randomNewFoods(1, 5, foodGroups.map(_.id))
  val referenceCategories = randomNewCategories(1, 5)

  val testFoods = randomNewFoods(1, 5, foodGroups.map(_.id))

  val testAssocFoods = randomAssociatedFoodsFor(testFoods.map(_.code), referenceFoods.map(_.code), referenceCategories.map(_.code))

  override def beforeAll() = {
    assert(service.createLocale(testLocale) === Right(()))
    assert(service.createFoodGroups(foodGroups) === Right(()))
    assert(service.createFoods(referenceFoods) === Right(()))
    assert(service.createFoods(testFoods) === Right(()))
    assert(service.createCategories(referenceCategories) === Right(()))
  }

  override def afterAll() = {
    assert(service.deleteAllFoodGroups() === Right(()))
    assert(service.deleteAllFoods() === Right(()))
    assert(service.deleteAllCategories() === Right(()))
    assert(service.deleteLocale(testLocale.id) === Right(()))
  }

  test("Create associated foods") {
    assert(service.createAssociatedFoods(testAssocFoods, testLocale.id) === Right(()))
  }

  test("Attempt to create associated foods for undefined parent food") {
    val foods = randomAssociatedFoodsFor(IndexedSeq(undefinedCode), referenceFoods.map(_.code), referenceCategories.map(_.code))

    if (foods(undefinedCode).length > 0) // due to implementetion specifics this error will not be generated if there are no assoc foods 
      assert(service.createAssociatedFoods(foods, testLocale.id) === Left(ParentRecordNotFound))

  }

  test("Attempt to create associated foods for undefined locale") {
    if (testAssocFoods.size > 0)
      assert(service.createAssociatedFoods(testAssocFoods, undefinedLocaleId) === Left(UndefinedLocale))
  }

  test("Get associated foods with headers") {

    def expected = testAssocFoods.mapValues {
      _.map {
        assocFood =>
          assocFood.foodOrCategoryCode match {
            case Left(code) => AssociatedFoodWithHeader(Left(referenceFoods.find(_.code == code).get.toHeader), assocFood.promptText, assocFood.linkAsMain, assocFood.genericName)
            case Right(code) => AssociatedFoodWithHeader(Right(referenceCategories.find(_.code == code).get.toHeader), assocFood.promptText, assocFood.linkAsMain, assocFood.genericName)
          }
      }
    }

    expected.keySet.foreach {
      code =>
        assert(service.getAssociatedFoodsWithHeaders(code, testLocale.id) === Right(expected(code)))
    }
  }

  test("Attempt to get associated foods for undefined food") {
    assert(service.getAssociatedFoodsWithHeaders(undefinedCode, testLocale.id) === Left(RecordNotFound))
  }

  test("Attempt to get associated foods for undefined locale") {
    assert(service.getAssociatedFoodsWithHeaders(testFoods(0).code, undefinedLocaleId) === Left(UndefinedLocale))
  }

  test("Delete associated foods") {
    assert(service.deleteAllAssociatedFoods(testLocale.id) === Right(()))
    assert(service.getAssociatedFoodsWithHeaders(testFoods(0).code, testLocale.id) === Right(Seq()))
  }
}