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
import uk.ac.ncl.openlab.intake24.NewMainFoodRecord
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService
import uk.ac.ncl.openlab.intake24.NewCategory
import uk.ac.ncl.openlab.intake24.AssociatedFoodWithHeader
import uk.ac.ncl.openlab.intake24.FoodHeader
import uk.ac.ncl.openlab.intake24.CategoryHeader
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.ParentRecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.user.DrinkwareService
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.DrinkwareAdminService
import uk.ac.ncl.openlab.intake24.DrinkwareSet
import uk.ac.ncl.openlab.intake24.VolumeFunction
import uk.ac.ncl.openlab.intake24.DrinkScale
import uk.ac.ncl.openlab.intake24.DrinkwareHeader

@DoNotDiscover
class DrinkwareAdminSuite(service: DrinkwareAdminService) extends FunSuite {

  /*  def listDrinkwareSets(): Either[DatabaseError, Map[String, DrinkwareHeader]]
  
  def deleteAllDrinkwareSets(): Either[DatabaseError, Unit]
  
  def createDrinkwareSets(sets: Seq[DrinkwareSet]): Either[CreateError, Unit]*/

  val scale1 = DrinkScale(1, "image.jpg", "image.jpg", 100, 100, 100, 100, VolumeFunction(Seq((1.0, 2.0), (2.0, 3.0), (4.0, 5.0))))
  val scale2 = DrinkScale(2, "image2.jpg", "image2.jpg", 200, 200, 100, 100, VolumeFunction(Seq((1.0, 2.0))))

  val set1 = DrinkwareSet("set1", "description", "ha ha", Seq(scale1, scale2))
  val set2 = DrinkwareSet("set2", "description", "ho ho", Seq(scale1))

  test("Create drinkware sets") {
    assert(service.createDrinkwareSets(Seq(set1, set2)) === Right(()))
  }
  
  test("Attempt to create a drinkware set with duplicate id") {
    assert(service.createDrinkwareSets(Seq(set1)) === Left(DuplicateCode))
  }

  test("List all drinkware sets") {
    assert(service.listDrinkwareSets() === Right(Map(set1.id -> DrinkwareHeader(set1.id, set1.description), set2.id -> DrinkwareHeader(set2.id, set2.description))))
  }
  
  test("Get a drinkware set") {
    assert(service.getDrinkwareSet(set1.id) === Right(set1))
    assert(service.getDrinkwareSet(set2.id) === Right(set2))
  }
  
  test("Attempt to get an undefined drinkware set") {
    assert(service.getDrinkwareSet("no_such_set") === Left(RecordNotFound))
  }

  test("Delete drinkware sets") {
    assert(service.deleteAllDrinkwareSets() === Right(()))
    assert(service.listDrinkwareSets() === Right(Map()))
  }

}