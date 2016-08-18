package uk.ac.ncl.openlab.intake24.foodsql.test

import org.scalatest.FunSuite
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import org.scalatest.DoNotDiscover
import uk.ac.ncl.openlab.intake24.AsServedImage
import uk.ac.ncl.openlab.intake24.AsServedSet

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UndefinedLocale
import org.scalatest.BeforeAndAfterAll
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.Locale
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.FoodGroupLocal
import uk.ac.ncl.openlab.intake24.FoodGroupRecord

@DoNotDiscover
class FoodGroupsAdminSuite(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll {

  val locale_id = SharedObjects.testLocale.id
  
  val fg1 = FoodGroupMain(1, "Group 1")
  val fg2 = FoodGroupMain(2, "Group 1")

  val fgl1_name = "Группа 1"
  val fgl2_name = "Группа 2"

  val fgl1 = FoodGroupLocal(Some(fgl1_name))
  val fgl2 = FoodGroupLocal(Some(fgl2_name))

  val emptyfgl = FoodGroupLocal(None)

  val localGroups = Map(fg1.id -> fgl1_name, fg2.id -> fgl2_name)

  val allGroupsWithoutLocal = Map(fg1.id -> FoodGroupRecord(fg1, emptyfgl), fg2.id -> FoodGroupRecord(fg2, emptyfgl))
  val allGroups = Map(fg1.id -> FoodGroupRecord(fg1, fgl1), fg2.id -> FoodGroupRecord(fg2, fgl2))

  override def beforeAll() = {
    service.createLocale(SharedObjects.testLocale)
  }

  override def afterAll() = {
    service.deleteLocale(locale_id)
  }

  test("Create food groups") {
    assert(service.createFoodGroups(Seq(fg1, fg2)) === Right(()))
  }
  
  test("Attempt to create a food group with duplicate id") {
    assert(service.createFoodGroups(Seq(fg1)) === Left(DuplicateCode))
  }

  test("List food groups without local data") {
    assert(service.listFoodGroups(locale_id) === Right(allGroupsWithoutLocal))
  }

  test("Create local food groups") {
    assert(service.createLocalFoodGroups(localGroups, locale_id).isRight)
  }

  test("List food groups with local data") {
    assert(service.listFoodGroups(locale_id) === Right(allGroups))
  }

  test("List food groups with undefined locale") {
    assert(service.listFoodGroups("no_such_locale") === Left(UndefinedLocale))
  }

  test("Get food group") {
    assert(service.getFoodGroup(fg1.id, locale_id) === Right(FoodGroupRecord(fg1, fgl1)))
    assert(service.getFoodGroup(fg2.id, locale_id) === Right(FoodGroupRecord(fg2, fgl2)))
  }

  test("Delete all food groups") {
    assert(service.deleteAllFoodGroups().isRight)
    assert(service.listFoodGroups(locale_id) === Right(Map()))
  }

  // TODO: add tests for below methods (not used right now)
  /* 
  def deleteLocalFoodGroups(locale: String): Either[DatabaseError, Unit]

  def createLocalFoodGroups(localFoodGroups: Map[Int, String], locale: String): Either[DatabaseError, Unit]  
   */
}