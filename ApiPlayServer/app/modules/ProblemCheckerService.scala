package modules

import models.FoodProblem
import models.CategoryProblem
import models.RecursiveCategoryProblems

trait ProblemCheckerService {
  def foodProblems(code: String, locale: String): Seq[FoodProblem]
  def categoryProblems(code: String, locale: String): Seq[CategoryProblem]
  def recursiveCategoryProblems(code: String, locale: String, maxReturnedProblems: Int): RecursiveCategoryProblems
}
