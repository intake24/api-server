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

sealed trait AnyError {
  val exception: Throwable
}

sealed trait LocalLookupError extends LocalUpdateError

sealed trait LocaleError
  extends LocalLookupError
  with LocalUpdateError
  with LocalCreateError
  with LocalDeleteError
  with LocaleOrParentError {
  val exception: Throwable
}

sealed trait LookupError extends LocalLookupError with UpdateError {
  val exception: Throwable
}

sealed trait LocalDeleteError extends AnyError

sealed trait DeleteError extends LocalDeleteError

sealed trait LocalUpdateError extends LocalDependentUpdateError

sealed trait UpdateError
  extends LocalUpdateError
  with DependentUpdateError {
  val exception: Throwable
}

sealed trait LocalCreateError extends AnyError

sealed trait CreateError 
  extends LocalCreateError
  with DependentCreateError {
  val exception: Throwable
}

sealed trait ParentError
  extends DependentUpdateError
  with DependentCreateError
  with LocaleOrParentError {
  val exception: Throwable
}

sealed trait LocaleOrParentError
  extends LocalDependentUpdateError
  with LocalDependentCreateError

sealed trait LocalDependentUpdateError extends AnyError

sealed trait DependentUpdateError 
  extends LocalDependentUpdateError

sealed trait LocalDependentCreateError extends AnyError

sealed trait DependentCreateError extends LocalDependentCreateError

sealed trait NutrientMappingError {
  val exception: Throwable
}

case class UndefinedLocale(exception: Throwable) extends LocaleError

sealed trait RecordType

case class RecordNotFound(exception: Throwable)
  extends LookupError
  with NutrientMappingError
  with DeleteError
  with UpdateError
  with AnyError

case class VersionConflict(exception: Throwable) extends UpdateError

case class DuplicateCode(exception: Throwable) extends CreateError with UpdateError

case class ParentRecordNotFound(exception: Throwable) extends ParentError

case class IllegalParent(exception: Throwable) extends ParentError

case class TableNotFound(exception: Throwable) extends NutrientMappingError

case class DatabaseError(exception: Throwable)
  extends LocaleError
  with LookupError
  with DeleteError
  with UpdateError
  with CreateError
  with NutrientMappingError
  with ParentError
  with AnyError
