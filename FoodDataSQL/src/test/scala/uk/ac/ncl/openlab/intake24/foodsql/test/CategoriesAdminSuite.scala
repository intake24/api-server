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
import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import java.util.UUID
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.CategoryRecord

@DoNotDiscover
class CategoriesAdminSuite(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll {

  /*
  def categoryRecord(code: String, locale: String): Either[LocalLookupError, CategoryRecord]
  
  def isCategoryCodeAvailable(code: String): Either[DatabaseError, Boolean]
  def isCategoryCode(code: String): Either[DatabaseError, Boolean]
  
  def createCategory(newCategory: NewCategory): Either[CreateError, Unit]
  def createCategories(newCategories: Seq[NewCategory]): Either[CreateError, Unit]
  def createLocalCategories(localCategoryRecords: Map[String, LocalCategoryRecord], locale: String): Either[CreateError, Unit]
  
  def deleteAllCategories(): Either[DatabaseError, Unit]
  def deleteCategory(categoryCode: String): Either[DeleteError, Unit]

  def updateCategoryMainRecord(categoryCode: String, categoryMain: MainCategoryRecord): Either[UpdateError, Unit]
  def updateCategoryLocalRecord(categoryCode: String, locale: String, categoryLocal: LocalCategoryRecord): Either[LocalUpdateError, Unit]
    
  def addFoodToCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]
  def addSubcategoryToCategory(categoryCode: String, subcategoryCode: String): Either[UpdateError, Unit]
  def removeFoodFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]
  def removeSubcategoryFromCategory(categoryCode: String, foodCode: String): Either[UpdateError, Unit]  
   */

  val testLocale = SharedObjects.testLocale

  val testCat1 = SharedObjects.testCat1
  val testCat2 = SharedObjects.testCat2

  val catLocal1 = LocalCategoryRecord(None, Some("Категория 1"), Seq(SharedObjects.testPsm1))
  val catLocal2 = LocalCategoryRecord(None, Some("Категория 2"), Seq(SharedObjects.testPsm1, SharedObjects.testPsm2))

  val dummyVersion = UUID.randomUUID()

  override def beforeAll() = {
    assert(service.createLocale(testLocale) === Right(()))
  }

  override def afterAll() = {
    assert(service.deleteLocale(testLocale.id) === Right(()))
  }

  def overrideVersion[E](result: Either[E, CategoryRecord]) = result.right.map {
    record => CategoryRecord(record.main.copy(version = dummyVersion), record.local.copy(version = Some(dummyVersion)))
  }

  test("Create single category") {
    assert(service.createCategory(testCat1) === Right(()))
  }

  test("Attempt to create a category with invalid code") {
    // Too short
    assert(service.createCategory(testCat1.copy(code = "1")).isLeft)
    // Too long
    assert(service.createCategory(testCat1.copy(code = "1234567890")).isLeft)
  }

  test("Attempt to create a category with duplicate code") {
    assert(service.createCategory(testCat1) === Left(DuplicateCode))
  }

  test("Test if category code exists") {
    assert(service.isCategoryCode(testCat1.code) === Right(true))
  }

  test("Test if category code is available") {
    assert(service.isCategoryCodeAvailable("MEGACODE") === Right(true))
  }

  test("Delete single category") {
    assert(service.deleteCategory(testCat1.code) === Right(()))
    assert(service.isCategoryCode(testCat1.code) === Right(false))
  }

  test("Attempt to delete undefined category") {
    assert(service.deleteCategory("BADCODE") === Left(RecordNotFound))
  }

  test("Batch create categories") {
    assert(service.createCategories(Seq(testCat1, testCat2)) === Right(()))
  }

  test("Batch create local category records") {
    assert(service.createLocalCategories(Map(testCat1.code -> catLocal1, testCat2.code -> catLocal2), testLocale.id) === Right(()))
  }
  
  test("Attempt to batch create duplicate codes") {
    assert(service.createLocalCategories(Map(testCat1.code -> catLocal1, testCat2.code -> catLocal2), testLocale.id) === Left(DuplicateCode))
  }

  test("Get category record") {
    val expected1 = CategoryRecord(MainCategoryRecord(dummyVersion, testCat1.code, testCat1.englishDescription, testCat1.isHidden, testCat1.attributes),
      LocalCategoryRecord(Some(dummyVersion), catLocal1.localDescription, catLocal1.portionSize))

    val expected2 = CategoryRecord(MainCategoryRecord(dummyVersion, testCat2.code, testCat2.englishDescription, testCat2.isHidden, testCat2.attributes),
      LocalCategoryRecord(Some(dummyVersion), catLocal2.localDescription, catLocal2.portionSize))

    assert(overrideVersion(service.getCategoryRecord(testCat1.code, testLocale.id)) === Right(expected1))
    assert(overrideVersion(service.getCategoryRecord(testCat2.code, testLocale.id)) === Right(expected2))
  }
  
  test("Attempt to get a category record for undefined category") {
    assert(service.getCategoryRecord("BADCODE", testLocale.id) === Left(RecordNotFound))
  }
  
  test("Attempt to get a category record for undefined locale") {
    assert(service.getCategoryRecord(testCat1.code, "no_such_locale") === Left(UndefinedLocale))
  }

  test("Update main record") {

    // Get current version and update
    assert(service.getCategoryRecord(testCat1.code, testLocale.id).right.flatMap {
      catRec =>
        service.updateCategoryMainRecord(catRec.main.code, catRec.main.copy(englishDescription = "Hello", code = "TEST123"))
    } === Right(()))

    // Check upated description
    assert(service.getCategoryRecord("TEST123", testLocale.id).right.map {
      rec => rec.main.englishDescription
    } === Right("Hello"))
  }

}