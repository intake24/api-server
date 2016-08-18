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
class BrandNamesAdminSuite(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll {

  /*
  def deleteAllBrandNames(locale: String): Either[LocaleError, Unit]
  
  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[LocalDependentCreateError, Unit] 
   */

  val testLocale = SharedObjects.testLocale

  val brands = Seq("Brand 1", "Brand 2", "Brand 3")

  val brandsMap = Map(SharedObjects.testFood1.code -> brands, SharedObjects.testFood2.code -> brands)

  override def beforeAll() = {
    assert(service.createLocale(testLocale) === Right(()))
    assert(service.createFoodGroups(SharedObjects.testFoodGroups) === Right(()))
    assert(service.createFood(SharedObjects.testFood1) === Right(()))
    assert(service.createFood(SharedObjects.testFood2) === Right(()))
  }
  
  override def afterAll() = {
    assert(service.deleteAllFoodGroups() === Right(()))
    assert(service.deleteAllFoods() === Right(()))
    assert(service.deleteLocale(testLocale.id) === Right(()))
  }

  test("Create brand names") {
    assert(service.createBrandNames(brandsMap, testLocale.id) === Right(()))
  }

  test("Attempt to create brand names for undefined locale") {
    assert(service.createBrandNames(brandsMap, "no_such_locale") === Left(UndefinedLocale))
  }

  test("Attempt to create brand names for undefined parent food") {
    assert(service.createBrandNames(Map("BADF00D" -> brands), testLocale.id) === Left(ParentRecordNotFound))
  }

  test("Get brand names") {
    assert(service.getBrandNames(SharedObjects.testFood1.code, testLocale.id) === Right(brands))
  }
  
  test("Attempt to delete brand names for undefined locale") {
    assert(service.deleteAllBrandNames("no_such_locale") === Left(UndefinedLocale))
  }

  test("Delete brand names") {
    assert(service.deleteAllBrandNames(testLocale.id) === Right(()))
    assert(service.getBrandNames(SharedObjects.testFood1.code, testLocale.id) === Right(Seq()))
  }
}