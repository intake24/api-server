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
class AssociatedFoodsAdminSuite(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll {
  
  /*
   *   def associatedFoods(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFoodWithHeader]]
  def updateAssociatedFoods(foodCode: String, locale: String, associatedFoods: Seq[AssociatedFood]): Either[LocalLookupError, Unit]
  
  def deleteAllAssociatedFoods(locale: String): Either[DatabaseError, Unit]
  def createAssociatedFoods(assocFoods: Map[String, Seq[AssociatedFood]], locale: String): Either[DatabaseError, Unit]
   */
  
  
  val testFood1 = SharedObjects.testFood1
  val testCat1 = SharedObjects.testCat1
  
  val testFoods = Seq(testFood1)
  
  val testLocale = SharedObjects.testLocale
  
  val af1 = AssociatedFood(Left(testFood1.code), "Prompt text 123", true, "abc")
  val af2 = AssociatedFood(Right(testCat1.code), "Prompt text 456", false, "xyz")
  
  val afmap = Map(testFood1.code -> Seq(af1, af2))
  
  override def beforeAll() = {
    assert(service.createLocale(testLocale) === Right(()))
    assert(service.createFoodGroups(SharedObjects.testFoodGroups) === Right(()))
    assert(service.createFood(testFood1) === Right(()))
    assert(service.createCategory(testCat1) === Right(()))
  }
  
  override def afterAll() = {
    assert(service.deleteAllFoodGroups() === Right(()))
    assert(service.deleteAllFoods() === Right(()))
    assert(service.deleteLocale(testLocale.id) === Right(()))
  }
  
  test("Create an empty associated foods list") {
    assert(service.createAssociatedFoods(Map(), testLocale.id) === Right(()))
  }
  
  test("Create associated foods") {
    assert(service.createAssociatedFoods(afmap, testLocale.id) === Right(()))
  }
  
  test("Attempt to create associated foods for undefined parent food") {
    assert(service.createAssociatedFoods(Map("BADF00D" -> Seq(af1, af2)), testLocale.id) === Left(ParentRecordNotFound))
  }
  
  test("Attempt to create associated foods for undefined locale") {
    assert(service.createAssociatedFoods(afmap, "no_such_locale") === Left(UndefinedLocale))
  }
  
  test("Get associated foods with headers") {
    
    val expected1 = AssociatedFoodWithHeader(Left(FoodHeader(testFood1.code, testFood1.englishDescription, None, None)), af1.promptText, af1.linkAsMain, af1.genericName)
    val expected2 = AssociatedFoodWithHeader(Right(CategoryHeader(testCat1.code, testCat1.englishDescription, None, false)), af2.promptText, af2.linkAsMain, af2.genericName)
    
    assert(service.getAssociatedFoodsWithHeaders(testFood1.code, testLocale.id) === Right(Seq(expected1, expected2)))
  }
  
  test("Attempt to get associated foods for undefined food") {
    assert(service.getAssociatedFoodsWithHeaders("no_such_food", testLocale.id) === Left(RecordNotFound))
  }

  test("Attempt to get associated foods for undefined locale") {
    assert(service.getAssociatedFoodsWithHeaders(testFood1.code, "no_such_locale") === Left(UndefinedLocale))
  }
  
  test("Delete associated foods") {
    assert(service.deleteAllAssociatedFoods(testLocale.id) === Right(()))
    assert(service.getAssociatedFoodsWithHeaders(testFood1.code, testLocale.id) === Right(Seq()))
  }
}