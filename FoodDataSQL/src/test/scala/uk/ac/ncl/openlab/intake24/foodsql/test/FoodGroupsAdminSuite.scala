package uk.ac.ncl.openlab.intake24.foodsql.test

import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FunSuite}
import uk.ac.ncl.openlab.intake24.errors.{DuplicateCode, UndefinedLocale}
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.{FoodGroupLocal, FoodGroupRecord}

@DoNotDiscover
class FoodGroupsAdminSuite(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll with RandomData with FixedData {

  val foodGroups = randomFoodGroups(2, 10)
  val localFoodGorups = randomLocalFoodGroups(foodGroups)

  val foodGroupRecordsNoLocal = foodGroups.map {
    group => group.id -> FoodGroupRecord(group, FoodGroupLocal(None))
  }.toMap

  val foodGroupRecords = foodGroups.map {
    group => group.id -> FoodGroupRecord(group, localFoodGorups(group.id))
  }.toMap

  override def beforeAll() = {
    service.createLocale(testLocale)
  }

  override def afterAll() = {
    service.deleteLocale(testLocale.id)
  }

  test("Create food groups") {
    assert(service.createFoodGroups(foodGroups) === Right(()))
  }

  test("Attempt to create a food group with duplicate id") {
    assert(service.createFoodGroups(foodGroups.take(1)) === Left(DuplicateCode))
  }

  test("List food groups without local data") {
    assert(service.listFoodGroups(testLocale.id) === Right(foodGroupRecordsNoLocal))
  }

  test("Create local food groups") {
    assert(service.createLocalFoodGroups(localFoodGorups, testLocale.id).isRight)
  }

  test("List food groups with local data") {
    assert(service.listFoodGroups(testLocale.id) === Right(foodGroupRecords))
  }

  test("List food groups with undefined locale") {
    assert(service.listFoodGroups(undefinedLocaleId) === Left(UndefinedLocale))
  }

  test("Get food group") {

    foodGroups.foreach {
      group =>
        assert(service.getFoodGroup(group.id, testLocale.id) === Right(FoodGroupRecord(group, localFoodGorups(group.id))))

    }
  }

  test("Delete all food groups") {
    assert(service.deleteAllFoodGroups().isRight)
    assert(service.listFoodGroups(testLocale.id) === Right(Map()))
  }

  // TODO: add tests for below methods (not used right now)
  /* 
  def deleteLocalFoodGroups(locale: String): Either[DatabaseError, Unit]
    
   */
}