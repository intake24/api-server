package modules

import models.{CategoryProblem, FoodProblem, RecursiveCategoryProblems}
import uk.ac.ncl.openlab.intake24.errors.LocalLookupError

trait ProblemCheckerService {

  def enablePrecacheWarnings(): Unit
  def disablePrecacheWarnings(): Unit

  def getFoodProblems(code: String, locale: String): Either[LocalLookupError, Seq[FoodProblem]]
  def getCategoryProblems(code: String, locale: String): Either[LocalLookupError, Seq[CategoryProblem]]
  def getRecursiveCategoryProblems(code: String, locale: String, maxReturnedProblems: Int): Either[LocalLookupError, RecursiveCategoryProblems]
}
