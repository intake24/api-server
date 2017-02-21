package uk.ac.ncl.openlab.intake24.foodsql.test

import org.scalatest.FunSuite

import org.scalatest.DoNotDiscover
import uk.ac.ncl.openlab.intake24.AsServedImageV1
import uk.ac.ncl.openlab.intake24.AsServedSetV1

import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import org.scalatest.BeforeAndAfterAll
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.NewMainFoodRecord
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.CategoryHeader

import uk.ac.ncl.openlab.intake24.LocalCategoryRecord
import java.util.UUID
import uk.ac.ncl.openlab.intake24.MainCategoryRecord
import uk.ac.ncl.openlab.intake24.CategoryRecord
import uk.ac.ncl.openlab.intake24.CategoryContents

@DoNotDiscover
class FoodBrowsingAdminSuite(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll with FixedData with RandomData {

  /*
   
    broken :(
  
  def uncategorisedFoods(locale: String): Either[LocaleError, Seq[FoodHeader]]

  def rootCategories(locale: String): Either[LocaleError, Seq[CategoryHeader]]

  def categoryContents(code: String, locale: String): Either[LocalLookupError, CategoryContents]

  def foodParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]

  def foodAllCategoriesCodes(code: String): Either[LookupError, Seq[String]]

  def foodAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
  
  def categoryParentCategories(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]]
  
  def categoryAllCategoriesCodes(code: String): Either[LookupError, Seq[String]]
  
  def categoryAllCategoriesHeaders(code: String, locale: String): Either[LocalLookupError, Seq[CategoryHeader]] 
  

  val foodGroups = randomFoodGroups(2, 10)
  val nutrientTables = randomNutrientTables(2, 10)
  val nutrientTableRecords = randomNutrientTableRecords(nutrientTables, 2, 10)

  val categories = randomNewCategories(10, 10)
  val categoriesLocal = randomLocalCategoryRecords(categories.map(_.code))
  val dummyVersion = UUID.randomUUID()

  val foods = randomNewFoods(2, 10, foodGroups.map(_.id))
  val localFoods = randomLocalFoods(foods.map(_.code), IndexedSeq(), IndexedSeq(), nutrientTableRecords)

  val foodHeaders = foods.map {
    food => FoodHeader(food.code, food.englishDescription, localFoods(food.code).localDescription, Some(localFoods(food.code).doNotUse))
  }.sortBy(_.code)

  val foodHeadersMap = foodHeaders.map {
    h => h.code -> h
  }.toMap

  val categoryHeaders = categories.map {
    cat => CategoryHeader(cat.code, cat.englishDescription, categoriesLocal(cat.code).localDescription, cat.isHidden)
  }.sortBy(_.code)

  val categoryHeadersMap = categoryHeaders.map {
    h => h.code -> h
  }.toMap

  override def beforeAll() = {
    assert(service.createLocale(testLocale) === Right(()))
    assert(service.createFoodGroups(foodGroups) === Right(()))

    nutrientTables.foreach {
      table => assert(service.createNutrientTable(table) === Right(()))
    }

    assert(service.createNutrientTableRecords(nutrientTableRecords) === Right(()))

  }

  override def afterAll() = {
    assert(service.deleteAllFoodGroups() === Right(()))
    assert(service.deleteAllNutrientTables() === Right(()))
    assert(service.deleteLocale(testLocale.id) === Right(()))
  }

  test("Create test food records") {
    assert(service.createCategories(categories) === Right(()))
    assert(service.createLocalCategoryRecords(categoriesLocal, testLocale.id) === Right(()))
    assert(service.createFoods(foods) === Right(()))
    assert(service.createLocalFoodRecords(localFoods, testLocale.id) === Right(()))
  }

  test("All foods should be uncategorised") {

    val uncategorisedFoods = service.getUncategorisedFoods(testLocale.id).right.map(_.sortBy(_.code))

    assert(uncategorisedFoods === Right(foodHeaders))
  }

  test("Some foods should be uncategorised") {
    assert(service.addFoodToCategory(categories.head.code, foods.head.code) === Right(()))

    val headers = foodHeaders.filterNot(_.code == foods.head.code)

    assert(service.getUncategorisedFoods(testLocale.id).right.map(_.sortBy(_.code)) === Right(headers))

  }

  test("No foods should be uncategorised") {
    foods.tail.foreach {
      food =>
        assert(service.addFoodToCategory(categories(0).code, food.code) === Right(()))
    }

    assert(service.getUncategorisedFoods(testLocale.id) === Right(Seq()))
  }

  // Category is not root if it has at least one non-hidden parent
  test("All categories should be root") {
    assert(service.getRootCategories(testLocale.id).right.map(_.sortBy(_.code)) === Right(categoryHeaders))
  }

  private def makeHidden(categoryCode: String) = {
    service.getCategoryRecord(categoryCode, testLocale.id).right.flatMap {
      record =>
        service.updateMainCategoryRecord(categoryCode, record.main.toUpdate.copy(isHidden = true))
    }
  }

  private def makeNonHidden(categoryCode: String) = {
    service.getCategoryRecord(categoryCode, testLocale.id).right.flatMap {
      record =>
        service.updateMainCategoryRecord(categoryCode, record.main.toUpdate.copy(isHidden = false))
    }
  }
  

  test("Category that has only hidden parents should be root") {
    assert(makeHidden(categories(0).code) === Right(()))
    assert(makeHidden(categories(1).code) === Right(()))

    assert(service.addSubcategoryToCategory(categories(0).code, categories(9).code) === Right(()))
    assert(service.addSubcategoryToCategory(categories(1).code, categories(9).code) === Right(()))

    assert(service.getRootCategories(testLocale.id).right.map(_.exists(_.code == categories(9).code)) === Right(true))
  }

  test("Category that has at least one non-hidden parent should not be root") {
    assert(makeNonHidden(categories(2).code) === Right(()))
    assert(service.addSubcategoryToCategory(categories(2).code, categories(9).code) === Right(()))

    assert(service.getRootCategories(testLocale.id).right.map(_.exists(_.code == categories(9).code)) === Right(false))
  }

   test("Restore hidden flags") {
     categories.foreach {
       cat =>
         val result = service.getCategoryRecord(cat.code, testLocale.id).right.flatMap {
           rec =>
             service.updateMainCategoryRecord(cat.code, rec.main.toUpdate.copy(isHidden = cat.isHidden))
         }
         assert(result === Right(()))
     }
  }

  def sorted(c: CategoryContents) =
    CategoryContents(c.foods.sortBy(_.code), c.subcategories.sortBy(_.code))

  test("Category contents") {

    val expected = CategoryContents(foodHeaders, Seq(categoryHeadersMap(categories(9).code)))

    assert(service.getCategoryContents(categories(0).code, testLocale.id).right.map(sorted(_)) === Right(sorted(expected)))
  }
  
  test("Add test food to more categories") {
    assert(service.addFoodToCategory(categories(1).code, foods(0).code) === Right(()))
  }

  test("Food parent categories") {
    val expected = Seq(categoryHeadersMap(categories(0).code), categoryHeadersMap(categories(1).code)).sortBy(_.code)
    assert(service.getFoodParentCategories(foods(0).code, testLocale.id).right.map(_.sortBy(_.code)) === Right(expected))
  }
  
  test("Food all categories codes") {
    assert(service.addSubcategoryToCategory(categories(3).code, categories(0).code) === Right(()))
    assert(service.addSubcategoryToCategory(categories(3).code, categories(1).code) === Right(()))
    assert(service.getFoodAllCategoriesCodes(foods(0).code) === Right(Set(categories(0).code, categories(1).code, categories(3).code)))
  }
  
  test("Food all categories headers") {
    val expected = Seq(categoryHeadersMap(categories(0).code), categoryHeadersMap(categories(1).code), categoryHeadersMap(categories(3).code)).sortBy(_.code)
    
    assert(service.getFoodAllCategoriesHeaders(foods(0).code, testLocale.id).right.map(_.sortBy(_.code)) === Right(expected))
  }
  
  test("Category parent categories") {
    val expected = Seq(categoryHeadersMap(categories(3).code))
    
    assert(service.getCategoryParentCategories(categories(0).code, testLocale.id) === Right(expected))
  }
  
  test("Category all categories codes") {
    assert(service.addSubcategoryToCategory(categories(4).code, categories(3).code) === Right(()))
    assert(service.addSubcategoryToCategory(categories(5).code, categories(4).code) === Right(()))
    
    val expected = Set(categories(3).code, categories(4).code, categories(5).code)
    
    assert(service.getCategoryAllCategoriesCodes(categories(0).code) === Right(expected))
  }
  
  test("Category all categories headers") {
    
    val expected = Seq(categoryHeadersMap(categories(3).code), categoryHeadersMap(categories(4).code), categoryHeadersMap(categories(5).code)).sortBy(_.code)
    
    assert(service.getCategoryAllCategoriesHeaders(categories(0).code, testLocale.id).right.map(_.sortBy(_.code)) === Right(expected))
  }
*/
}