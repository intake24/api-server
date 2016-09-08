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

sealed trait LocaleError
  extends LocalLookupError
  with LocalUpdateError
  with LocalCreateError
  with LocalDeleteError
  with LocaleOrParentError

sealed trait LookupError extends LocalLookupError with UpdateError

sealed trait LocalDeleteError

sealed trait DeleteError extends LocalDeleteError

sealed trait LocalUpdateError extends LocalDependentUpdateError

sealed trait UpdateError
  extends LocalUpdateError
  with DependentUpdateError

sealed trait LocalCreateError

sealed trait CreateError 
  extends LocalCreateError
  with DependentCreateError

sealed trait ParentError
  extends DependentUpdateError
  with DependentCreateError
  with LocaleOrParentError

sealed trait LocaleOrParentError
  extends LocalDependentUpdateError
  with LocalDependentCreateError

sealed trait LocalDependentUpdateError

sealed trait DependentUpdateError 
  extends LocalDependentUpdateError

sealed trait LocalDependentCreateError

sealed trait DependentCreateError extends LocalDependentCreateError

sealed trait NutrientMappingError

case object UndefinedLocale extends LocaleError

sealed trait RecordType

case object RecordNotFound
  extends LookupError
  with NutrientMappingError
  with DeleteError
  with UpdateError  

case object VersionConflict extends UpdateError

case object DuplicateCode extends CreateError with UpdateError

case object ParentRecordNotFound extends ParentError

case object IllegalParent extends ParentError

case object TableNotFound extends NutrientMappingError

case class DatabaseError(message: String, cause: Option[Throwable])
  extends LocaleError
  with LookupError
  with DeleteError
  with UpdateError
  with CreateError
  with NutrientMappingError
  with ParentError
