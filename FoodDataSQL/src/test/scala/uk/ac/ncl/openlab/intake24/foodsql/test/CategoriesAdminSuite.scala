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
class CategoriesAdminSuite(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll with FixedData with RandomData {

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

  val foodGroups = randomFoodGroups(2, 10)
  val foods = randomNewFoods(2, 10, foodGroups.map(_.id))
  val randomCategories = randomNewCategories(3, 10)
  val singleCategory = randomCategories.head
  val categories = randomCategories.tail
  val categoriesLocal = randomLocalCategoryRecords(categories.map(_.code))
  val dummyVersion = UUID.randomUUID()

  override def beforeAll() = {
    assert(service.createLocale(testLocale) === Right(()))
    assert(service.createFoodGroups(foodGroups) === Right(()))
    assert(service.createFoods(foods) === Right(()))
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
    assert(service.createCategory(singleCategory) === Right(()))
  }

  test("Attempt to create a category with invalid code") {
    // Too short
    assert(service.createCategory(singleCategory.copy(code = "1")).isLeft)
    // Too long
    assert(service.createCategory(singleCategory.copy(code = "1234567890")).isLeft)
  }

  test("Attempt to create a category with duplicate code") {
    assert(service.createCategory(singleCategory) === Left(DuplicateCode))
  }

  test("Test if category code exists") {
    assert(service.isCategoryCode(singleCategory.code) === Right(true))
  }

  test("Test if category code is available") {
    assert(service.isCategoryCodeAvailable(undefinedCode) === Right(true))
  }

  test("Delete single category") {
    assert(service.deleteCategory(singleCategory.code) === Right(()))
    assert(service.isCategoryCode(singleCategory.code) === Right(false))
  }

  test("Attempt to delete undefined category") {
    assert(service.deleteCategory(undefinedCode) === Left(RecordNotFound))
  }

  test("Batch create categories") {
    assert(service.createCategories(categories) === Right(()))
  }

  test("Attempt to batch create duplicate categories") {
    assert(service.createCategories(categories) === Left(DuplicateCode))
  }

  test("Batch create local category records") {
    assert(service.createLocalCategories(categoriesLocal, testLocale.id) === Right(()))
  }

  test("Attempt to batch create duplicate local records") {
    assert(service.createLocalCategories(categoriesLocal, testLocale.id) === Left(DuplicateCode))
  }

  test("Get category record") {
    categories.foreach {
      category =>

        val expected = CategoryRecord(MainCategoryRecord(dummyVersion, category.code, category.englishDescription, category.isHidden, category.attributes),
          LocalCategoryRecord(Some(dummyVersion), categoriesLocal(category.code).localDescription, categoriesLocal(category.code).portionSize))

        assert(overrideVersion(service.getCategoryRecord(category.code, testLocale.id)) === Right(expected))
    }
  }

  test("Attempt to get a category record for undefined category") {
    assert(service.getCategoryRecord(undefinedCode, testLocale.id) === Left(RecordNotFound))
  }

  test("Attempt to get a category record for undefined locale") {
    assert(service.getCategoryRecord(categories(0).code, undefinedLocaleId) === Left(UndefinedLocale))
  }

  test("Update main record") {

    val newCode = randomUniqueId(categories.map(_.code).toSet, randomCode)
    val newDescription = randomDescription
    val newAttributes = randomAttributes

    // Get current version and update
    assert(service.getCategoryRecord(categories(0).code, testLocale.id).right.flatMap {
      catRec =>
        service.updateCategoryMainRecord(catRec.main.code, catRec.main.copy(englishDescription = newDescription, code = newCode, attributes = newAttributes))
    } === Right(()))

    // Check upated description
    assert(service.getCategoryRecord(newCode, testLocale.id).right.map {
      rec => (rec.main.englishDescription, rec.main.attributes)
    } === Right((newDescription, newAttributes)))
  }

  test("Update local record") {

    val newPsm = randomPortionSizeMethods
    val newDescription = Some(randomDescription)

    // Get current version and update
    assert(service.getCategoryRecord(categories(1).code, testLocale.id).right.flatMap {
      catRec =>
        service.updateCategoryLocalRecord(categories(1).code, testLocale.id, catRec.local.copy(localDescription = newDescription, portionSize = newPsm))
    } === Right(()))

    // Check upated description
    assert(service.getCategoryRecord(categories(1).code, testLocale.id).right.map {
      rec => (rec.local.localDescription, rec.local.portionSize)
    } === Right((newDescription, newPsm)))
  }

  test("Attempt to update an undefined category main record") {
    assert(service.updateCategoryMainRecord(undefinedCode, MainCategoryRecord(dummyVersion, categories(1).code, categories(1).englishDescription, categories(1).isHidden, categories(1).attributes)) === Left(RecordNotFound))
  }

  test("Attempt to update an undefined category local record") {
    assert(service.updateCategoryLocalRecord(undefinedCode, testLocale.id, LocalCategoryRecord(Some(dummyVersion), categoriesLocal(categories(1).code).localDescription, categoriesLocal(categories(1).code).portionSize)) === Left(RecordNotFound))
  }

  test("Attempt to update an category local record for undefined locale") {
    assert(service.updateCategoryLocalRecord(categories(1).code, undefinedLocaleId, LocalCategoryRecord(Some(dummyVersion), categoriesLocal(categories(1).code).localDescription, categoriesLocal(categories(1).code).portionSize)) === Left(UndefinedLocale))
  }

  test("Recreate categories for following tests") {
    assert(service.deleteAllCategories() === Right(()))
    assert(service.createCategories(categories) === Right(()))
  }

  test("Add food to category") {    
    foods.foreach {
      food =>
        assert(service.addFoodToCategory(randomElement(categories).code, food.code) === Right(()))
    }
  }

  test("Add subcategory to category") {
    assert(service.addSubcategoryToCategory(categories(0).code, categories(1).code) === Right(()))
  }

  test("Attempt to add a category to itself") {
    assert(service.addSubcategoryToCategory(categories(1).code, categories(1).code).isLeft)
  }

  test("Attempt to create a cycle") {
    // no cycle checks right now :(
  }

  test("Clean up categories") {
    assert(service.deleteAllCategories() === Right(()))
  }

}