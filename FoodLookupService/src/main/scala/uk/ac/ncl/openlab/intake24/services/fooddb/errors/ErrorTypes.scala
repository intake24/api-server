/*
This file is part of Intake24.

Copyright 2015, 2016 Newcastle University.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package uk.ac.ncl.openlab.intake24.services.fooddb.errors

sealed trait NutrientMappingError

object NutrientMappingError

case object TableNotFound extends NutrientMappingError
case object RecordNotFound extends NutrientMappingError

sealed trait LocalFoodCodeError

sealed trait FoodDataError

sealed trait FoodCodeError

sealed trait LocaleError

sealed trait CategoryCodeError

sealed trait LocalCategoryCodeError

case object UndefinedCode extends FoodCodeError with UpdateError with LocalUpdateError with LocalFoodCodeError with CategoryCodeError with LocalCategoryCodeError

case object UndefinedLocale extends LocaleError with LocalFoodCodeError with LocalCategoryCodeError with LocalUpdateError

case object NoLocalDescription extends FoodDataError

sealed trait ResourceError

case object ResourceNotFound extends ResourceError

sealed trait UpdateError

sealed trait LocalUpdateError

case object VersionConflict extends UpdateError with LocalUpdateError

sealed trait CreateError

case object DuplicateCode extends CreateError

case class DatabaseError(message: String, cause: Option[Throwable]) extends UpdateError
  with FoodCodeError
  with LocalFoodCodeError
  with CategoryCodeError
  with LocalCategoryCodeError
  with FoodDataError
  with ResourceError
  with NutrientMappingError
  with CreateError
  with LocalUpdateError
  with LocaleError
