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
import play.api.{Configuration, Environment}
import scheduled.{PairwiseAssociationsRefresher, PairwiseAssociationsRefresherImpl}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.{PairwiseAssociationsDataService, PairwiseAssociationsService, PairwiseAssociationsServiceConfiguration}
import uk.ac.ncl.openlab.intake24.systemsql.pairwiseAssociations.{PairwiseAssociationsDataServiceImpl, PairwiseAssociationsServiceImpl}

class PairwiseAssociationsModule(env: Environment, config: Configuration) extends AbstractModule {

  @Provides
  @Singleton
  def pairwiseAssociationsServiceSettings(configuration: Configuration): PairwiseAssociationsServiceConfiguration =
    PairwiseAssociationsServiceConfiguration(
      configuration.get[Int]("intake24.pairwiseAssociations.minimumNumberOfSurveySubmissions"),
      configuration.get[Seq[String]]("intake24.pairwiseAssociations.ignoreSurveysContaining"),
      configuration.get[Int]("intake24.pairwiseAssociations.useAfterNumberOfTransactions"),
      configuration.get[Int]("intake24.pairwiseAssociations.rulesUpdateBatchSize"),
      configuration.get[String]("intake24.pairwiseAssociations.refreshAtTime"),
      configuration.get[Int]("intake24.pairwiseAssociations.minInputSearchSize")
    )

  def configure() = {
    // Pairwise services
    bind(classOf[PairwiseAssociationsDataService]).to(classOf[PairwiseAssociationsDataServiceImpl])
    bind(classOf[PairwiseAssociationsService]).to(classOf[PairwiseAssociationsServiceImpl])
    bind(classOf[PairwiseAssociationsRefresher]).to(classOf[PairwiseAssociationsRefresherImpl]).asEagerSingleton()
  }
}
