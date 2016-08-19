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
class AsServedImageAdminSuite(service: AsServedImageAdminService) extends FunSuite with RandomData {
  
  private val sets = randomAsServedSets
  
  private val headers = sets.map(_.toHeader).map(h => (h.id, h)).toMap
  
  test("Batch create as served images sets") {
    assert(service.createAsServedSets(sets) === Right(()))
  }
  
  test("Attempt to create a set with existing id") {
    assert(service.createAsServedSets(sets.take(1)) === Left(DuplicateCode))
  }
  
  test("Get all as served image sets") {
    assert(service.listAsServedSets() === Right(headers))
  }
  
  test("Get a defined as served set") {
    
    service.getAsServedSet(sets(0).id) match {
      case Left(error) => fail("Unexpected error: " + error)
      case Right(set) => {
        assert(set.id === sets(0).id)
        assert(set.images.sortBy(_.weight) === sets(0).images.sortBy(_.weight))
      }
    }    
  }
  
  test("Attempt to get an undefined as served set") {
    assert(service.getAsServedSet("no_such_set") === Left(RecordNotFound))
  }
  
  test("Delete all as served sets") {
    assert(service.deleteAllAsServedSets() === Right(()))
    assert(service.listAsServedSets() === Right(Map()))
  }
  
}