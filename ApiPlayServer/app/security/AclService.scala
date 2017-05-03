package security

import models.AccessSubject
import uk.ac.ncl.openlab.intake24.errors.AnyError

/**
  * Created by Tim Osadchiy on 03/05/2017.
  */
trait AclService {
  def canPatchUser[T](subject: AccessSubject, userId: Long): Either[AnyError, Boolean]
}
