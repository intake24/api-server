package uk.ac.ncl.openlab.intake24.services

import uk.ac.ncl.openlab.intake24.services.foodindex.IndexLookupResult

sealed trait UseInRecipes

object UseInRecipes {

  case object Anywhere extends UseInRecipes

  case object RegularFoodsOnly extends UseInRecipes

  case object RecipesOnly extends UseInRecipes

}

trait RecipesAttributeIndex {

  def filterForRecipes(indexLookupResult: IndexLookupResult): IndexLookupResult

  def filterForRegularFoods(indexLookupResult: IndexLookupResult): IndexLookupResult
}
