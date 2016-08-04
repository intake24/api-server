package uk.ac.ncl.openlab.intake24.services.fooddb.user

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.CodeError

trait BrandNamesService {
  
  def brandNames(foodCode: String, locale: String): Either[CodeError, Seq[String]]
}
