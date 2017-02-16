package uk.ac.ncl.openlab.intake24.foodsql.test

import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FunSuite}
import uk.ac.ncl.openlab.intake24.errors.{ParentRecordNotFound, UndefinedLocale}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService

@DoNotDiscover
class BrandNamesAdminSuite(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll with FixedData with RandomData {

  /*
  def deleteAllBrandNames(locale: String): Either[LocaleError, Unit]
  
  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[LocalDependentCreateError, Unit] 
   */

  val foodGroups = randomFoodGroups(2, 10)
  val foods = randomNewFoods(2, 10, foodGroups.map(_.id))
  val brandNames = randomBrandsFor(foods.map(_.code))

  override def beforeAll() = {
    assert(service.createLocale(testLocale) === Right(()))
    assert(service.createFoodGroups(foodGroups) === Right(()))
    assert(service.createFoods(foods) === Right(()))
  }

  override def afterAll() = {
    assert(service.deleteAllFoodGroups() === Right(()))
    assert(service.deleteAllFoods() === Right(()))
    assert(service.deleteLocale(testLocale.id) === Right(()))
  }

  test("Create brand names") {
    assert(service.createBrandNames(brandNames, testLocale.id) === Right(()))
  }

  test("Attempt to create brand names for undefined locale") {
    assert(service.createBrandNames(brandNames, undefinedLocaleId) === Left(UndefinedLocale))
  }

  test("Attempt to create brand names for undefined parent food") {
    assert(service.createBrandNames(Map(undefinedCode -> Seq(randomDescription)), testLocale.id) === Left(ParentRecordNotFound))
  }

  test("Get brand names") {

    brandNames.keySet.foreach {
      code =>
        assert(service.getBrandNames(code, testLocale.id) === Right(brandNames(code)))
    }
  }

  test("Attempt to delete brand names for undefined locale") {
    assert(service.deleteAllBrandNames(undefinedLocaleId) === Left(UndefinedLocale))
  }

  test("Delete brand names") {
    assert(service.deleteAllBrandNames(testLocale.id) === Right(()))
    assert(service.getBrandNames(foods(0).code, testLocale.id) === Right(Seq()))
  }
}