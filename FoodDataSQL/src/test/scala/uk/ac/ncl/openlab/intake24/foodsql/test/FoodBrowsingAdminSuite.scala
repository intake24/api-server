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
class FoodBrowsingAdminSuite(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll {

  /*
  
  def uncategorisedFoods(locale: String): Either[LocaleError, Seq[FoodHeader]]

  def rootCategories(locale: String): Either[LocaleError, Seq[CategoryHeader]]

  def categoryContents(code: String, locale: String): Either[LocalLookupError, CategoryContents]

  def foodParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]

  def foodAllCategoriesCodes(code: String): Either[LookupError, Seq[String]]

  def foodAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
  
  def categoryParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
  
  def categoryAllCategoriesCodes(code: String): Either[LookupError, Seq[String]]
  
  def categoryAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] 
   

  val testLocale = FixedData.testLocale

  val testCat1 = FixedData.testCat1
  val testCat2 = FixedData.testCat2
  
  val testFood1 = FixedData.testFood1

  val catLocal1 = LocalCategoryRecord(None, Some("Категория 1"), Seq(FixedData.testPsm1))
  val catLocal2 = LocalCategoryRecord(None, Some("Категория 2"), Seq(FixedData.testPsm1, FixedData.testPsm2))

  val dummyVersion = UUID.randomUUID()

  override def beforeAll() = {
    assert(service.createLocale(testLocale) === Right(()))
    assert(service.createFoodGroups(FixedData.testFoodGroups) === Right(()))
    assert(service.createFood(FixedData.testFood1) === Right(()))    
  }

  override def afterAll() = {
    assert(service.deleteLocale(testLocale.id) === Right(()))
    assert(service.deleteAllFoods() === Right(()))
    assert(service.deleteAllFoodGroups() === Right(()))
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
  
  test("Attempt to batch create duplicate categories") {
    assert(service.createCategories(Seq(testCat1, testCat2)) === Left(DuplicateCode))
  }

  test("Batch create local category records") {
    assert(service.createLocalCategories(Map(testCat1.code -> catLocal1, testCat2.code -> catLocal2), testLocale.id) === Right(()))
  }

  test("Attempt to batch create duplicate local records") {
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
        service.updateCategoryMainRecord(catRec.main.code, catRec.main.copy(englishDescription = "Hello", code = "TEST123", attributes = FixedData.someInheritableAttr))
    } === Right(()))

    // Check upated description
    assert(service.getCategoryRecord("TEST123", testLocale.id).right.map {
      rec => (rec.main.englishDescription, rec.main.attributes)
    } === Right(("Hello", FixedData.someInheritableAttr)))
  }

  test("Update local record") {
    
    val testPsm = Seq(FixedData.testPsm2, FixedData.testPsm3)
    
    // Get current version and update
    assert(service.getCategoryRecord("TEST123", testLocale.id).right.flatMap {
      catRec =>
        service.updateCategoryLocalRecord("TEST123", testLocale.id, catRec.local.copy(localDescription = Some("Bonjour"), portionSize = testPsm))
    } === Right(()))

    // Check upated description
    assert(service.getCategoryRecord("TEST123", testLocale.id).right.map {
      rec => (rec.local.localDescription, rec.local.portionSize)
    } === Right((Some("Bonjour"), testPsm)))
  }

  test("Attempt to update an undefined category main record") {
    assert(service.updateCategoryMainRecord(":D", MainCategoryRecord(dummyVersion, testCat2.code, testCat2.englishDescription, testCat2.isHidden, testCat2.attributes)) === Left(RecordNotFound))
  }
  
  test("Attempt to update an undefined category local record") {
    assert(service.updateCategoryLocalRecord(":D", testLocale.id, LocalCategoryRecord(Some(dummyVersion), catLocal2.localDescription, catLocal2.portionSize)) === Left(RecordNotFound))
  }
  
  test("Attempt to update an category local record for undefined locale") {
    assert(service.updateCategoryLocalRecord("TEST123", "no_such_locale", LocalCategoryRecord(Some(dummyVersion), catLocal2.localDescription, catLocal2.portionSize)) === Left(UndefinedLocale))
  }
  
  test("Recreate categories for following tests") {
    assert(service.deleteAllCategories() === Right(()))
    assert(service.createCategories(Seq(testCat1, testCat2)) === Right(()))
  }
  
  test("Add food to category") {
    assert(service.addFoodToCategory(testCat1.code, testFood1.code) === Right(()))
  }
  
  test("Add subcategory to category") {
    assert(service.addSubcategoryToCategory(testCat2.code, testCat1.code) === Right(()))
  }
  
  test("Attempt to add a category to itself") {
    assert(service.addSubcategoryToCategory(testCat2.code, testCat2.code).isLeft)
  }
  
  test("Attempt to create a cycle") {
    // no cycle checks right now :(
  }
  
  test("Clean up categories") {
    assert(service.deleteAllCategories() === Right(()))
  }
*/
}