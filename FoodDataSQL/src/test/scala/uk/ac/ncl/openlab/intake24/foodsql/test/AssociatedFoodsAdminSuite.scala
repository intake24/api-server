package uk.ac.ncl.openlab.intake24.foodsql.test

import org.scalatest.FunSuite
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import org.scalatest.DoNotDiscover
import uk.ac.ncl.openlab.intake24.AsServedImage
import uk.ac.ncl.openlab.intake24.AsServedSet

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AssociatedFoodsAdminService
import org.scalatest.BeforeAndAfterAll
import uk.ac.ncl.openlab.intake24.AssociatedFood
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodsAdminService
import uk.ac.ncl.openlab.intake24.FoodGroupRecord
import uk.ac.ncl.openlab.intake24.NewFood
import uk.ac.ncl.openlab.intake24.InheritableAttributes
import uk.ac.ncl.openlab.intake24.FoodGroupMain
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodDatabaseAdminService

@DoNotDiscover
class AssociatedFoodsAdminSute(service: FoodDatabaseAdminService) extends FunSuite with BeforeAndAfterAll {
  
  /*
   *   def associatedFoods(foodCode: String, locale: String): Either[LocalLookupError, Seq[AssociatedFoodWithHeader]]
  def updateAssociatedFoods(foodCode: String, locale: String, associatedFoods: Seq[AssociatedFood]): Either[LocalLookupError, Unit]
  
  def deleteAllAssociatedFoods(locale: String): Either[DatabaseError, Unit]
  def createAssociatedFoods(assocFoods: Map[String, Seq[AssociatedFood]], locale: String): Either[DatabaseError, Unit]
   */
  
  val testFoodGroups = Seq(FoodGroupMain(1, "Test food group"))
  
  val testFoods = Seq(NewFood("TF01", "Test food 1", 1, InheritableAttributes(None, None, None)))
  
  val af1 = AssociatedFood(Left("food1"), "Prompt text 123", true, "abc")
  val af2 = AssociatedFood(Right("cat1"), "Prompt text 456", false, "xyz")
  
  override def beforeAll() = {
    (for (
        _ <- service.createFoodGroups(testFoodGroups).right;
        _ <- service.createFoods(testFoods).right) yield ()) match {      
      case Left(e) => throw new RuntimeException(s"Could not init test data: ${e.message}")
      case _ => ()
    }
  }
  
  override def afterAll() = {
    (for (
        _ <- service.deleteAllFoodGroups().right;
        _ <- service.deleteAllFoods().right) yield ()) match {      
      case Left(e) => throw new RuntimeException(s"Could not clean up test data: ${e.message}")
      case _ => ()
    }    
  }
  
  test("Batch create with an empty associated foods list") {
    assert(service.createAssociatedFoods(Map(), "en_GB").isRight)
  }
  
}