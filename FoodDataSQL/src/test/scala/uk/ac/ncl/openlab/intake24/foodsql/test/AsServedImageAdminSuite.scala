package uk.ac.ncl.openlab.intake24.foodsql.test

import org.scalatest.FunSuite
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.AsServedImageAdminService
import org.scalatest.DoNotDiscover
import uk.ac.ncl.openlab.intake24.AsServedImage
import uk.ac.ncl.openlab.intake24.AsServedSet

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DuplicateCode
import uk.ac.ncl.openlab.intake24.services.fooddb.admin.FoodGroupsAdminService

@DoNotDiscover
class AsServedImageAdminSuite(service: AsServedImageAdminService) extends FunSuite {
  
  private val img11 = AsServedImage("1_1.jpg", 10)
  private val img12 = AsServedImage("1_2.jpg", 20)
  private val img13 = AsServedImage("1_3.jpg", 30)
  
  private val img21 = AsServedImage("2_1.jpg", 15)
  private val img22 = AsServedImage("2_2.jpg", 50)
  private val img23 = AsServedImage("2_3.jpg", 35)
    
  private val set1 = AsServedSet("set1", "Test set 1", Seq(img11, img12, img13)) 
  private val set2 = AsServedSet("set2", "Test set 2", Seq(img21, img22, img23))
  private val set3 = AsServedSet("set3", "Test set 3", Seq())
  
  private val allSets = Seq(set1, set2, set3)
  
  private val headers = allSets.map(_.toHeader).map(h => (h.id, h)).toMap
  
  test("Batch create with an empty set list") {
    assert(service.createAsServedSets(Seq()).isRight)
  }
  
  test("Create a set with an empty image list") {
    assert(service.createAsServedSets(Seq(set3)).isRight)
  }
  
  test("Batch create as served images sets") {
    assert(service.createAsServedSets(Seq(set1, set2)).isRight)
  }
  
  test("Attempt to create a set with existing id") {
    assert(service.createAsServedSets(Seq(AsServedSet("set3", "Duplicate", Seq()))) === Left(DuplicateCode))
  }
  
  test("Get all as served image sets") {
    assert(service.listAsServedSets() === Right(headers))
  }
  
  test("Get a defined as served set") {
    
    service.getAsServedSet("set1") match {
      case Left(_) => fail("Unexpected error")
      case Right(set) => {
        assert(set.id === set1.id)
        assert(set.images.sortBy(_.weight) === set1.images)
      }
    }    
  }
  
  test("Get an undefined as served set") {
    assert(service.getAsServedSet("no_such_set") === Left(RecordNotFound))
  }
  
}