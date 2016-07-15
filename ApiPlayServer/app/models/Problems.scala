package models

case class CategoryProblem(categoryCode: String, categoryName: String, problemCode: String)

case class FoodProblem(foodCode: String, foodName: String, problemCode: String)

case class RecursiveCategoryProblems(foodProblems: Seq[FoodProblem], categoryProblems: Seq[CategoryProblem]) {
  def count = foodProblems.size + categoryProblems.size
  def ++(other: RecursiveCategoryProblems) = RecursiveCategoryProblems(foodProblems ++ other.foodProblems, categoryProblems ++ other.categoryProblems)
}
