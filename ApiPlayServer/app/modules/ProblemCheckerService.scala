package modules

import models.FoodProblem
import models.CategoryProblem
import models.RecursiveCategoryProblems
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LocalLookupError

trait ProblemCheckerService {
  def getFoodProblems(code: String, locale: String): Either[LocalLookupError, Seq[FoodProblem]]
  def getCategoryProblems(code: String, locale: String): Either[LocalLookupError, Seq[CategoryProblem]]
  def getRecursiveCategoryProblems(code: String, locale: String, maxReturnedProblems: Int): Either[LocalLookupError, RecursiveCategoryProblems]
}
