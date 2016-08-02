package uk.ac.ncl.openlab.intake24.huh

import uk.ac.ncl.openlab.intake24.UserCategoryHeader
import uk.ac.ncl.openlab.intake24.UserFoodHeader

case class UserAssociatedFood (foodOrCategory: Either[UserFoodHeader, UserCategoryHeader], promptText: String, linkAsMain: Boolean, genericName: String)
