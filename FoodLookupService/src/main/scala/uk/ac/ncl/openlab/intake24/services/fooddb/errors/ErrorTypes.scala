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

sealed trait LocalLookupError extends LocalUpdateError

sealed trait LocaleError extends LocalLookupError with LocalUpdateError with LocalCreateError with LocalDeleteError with LocalDependentCreateError with LocalDependentUpdateError

sealed trait LookupError extends LocalLookupError

sealed trait LocalDeleteError

sealed trait DeleteError extends LocalDeleteError

sealed trait LocalUpdateError

sealed trait UpdateError extends LocalUpdateError with DependentUpdateError

sealed trait LocalCreateError

sealed trait CreateError extends LocalCreateError

sealed trait LocalDependentUpdateError

sealed trait DependentUpdateError extends LocalDependentUpdateError

sealed trait LocalDependentCreateError

sealed trait DependentCreateError extends LocalDependentCreateError


sealed trait NutrientMappingError

case object UndefinedLocale extends LocaleError

sealed trait RecordType

object RecordType {
  
  case object FoodGroup extends RecordType

  case object AsServedSet extends RecordType

  case object GuideImage extends RecordType

  case object DrinkwareSet extends RecordType
  
  case object NutrientTable extends RecordType

  case object NutrientTableRecord extends RecordType

  case object Food extends RecordType

  case object Category extends RecordType
  
  case object Locale extends RecordType
}

case class RecordNotFound(recordType: RecordType, code: String) extends LookupError with NutrientMappingError with DeleteError with UpdateError with DependentUpdateError

case object VersionConflict extends UpdateError

case object DuplicateCode extends CreateError with UpdateError with DependentCreateError with DependentUpdateError

case object ParentRecordNotFound extends DependentCreateError with DependentUpdateError

case object TableNotFound extends NutrientMappingError

case class DatabaseError(message: String, cause: Option[Throwable])
  extends LocaleError
  with LookupError
  with LocalLookupError
  with DeleteError
  with LocalDeleteError
  with UpdateError
  with LocalUpdateError
  with CreateError
  with LocalCreateError
  with NutrientMappingError
  with DependentCreateError
  with LocalDependentCreateError

object test {
  val q: LocaleError = UndefinedLocale
  val x: LocalLookupError = q

}