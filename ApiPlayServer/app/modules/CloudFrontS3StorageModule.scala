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

package modules

import com.google.inject.{AbstractModule, Provides, Singleton}
import play.api.Configuration
import uk.ac.ncl.openlab.intake24.services.fooddb.images.{ImageStorageCloudFrontS3, ImageStorageService, CloudFrontS3ImageStorageSettings}

class CloudFrontS3StorageModule extends AbstractModule {

  @Provides
  @Singleton
  def settings(configuration: Configuration): CloudFrontS3ImageStorageSettings =
    CloudFrontS3ImageStorageSettings(
      configuration.get[String]("intake24.images.S3Storage.bucketName"),
      configuration.get[String]("intake24.images.S3Storage.pathPrefix"),
      configuration.get[String]("intake24.images.CloudFront.imageUrl"),
      configuration.get[String]("intake24.images.CloudFront.pathPrefix"))

  override def configure() = {
    bind(classOf[ImageStorageService]).to(classOf[ImageStorageCloudFrontS3])
  }
}
