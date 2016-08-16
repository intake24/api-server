package uk.ac.ncl.openlab.intake24.services.fooddb.admin

import uk.ac.ncl.openlab.intake24.services.fooddb.user.BrandNamesService
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.DatabaseError

trait BrandNamesAdminService extends BrandNamesService {
  
  def deleteAllBrandNames(): Either[DatabaseError, Unit]
  
  def createBrandNames(brandNames: Map[String, Seq[String]], locale: String): Either[DatabaseError, Unit]
  
}
