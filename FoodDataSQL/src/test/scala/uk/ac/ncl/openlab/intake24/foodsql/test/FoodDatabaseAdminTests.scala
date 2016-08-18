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

package uk.ac.ncl.openlab.intake24.foodsql.test

import com.google.inject.Inject
import com.google.inject.Singleton
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

import uk.ac.ncl.openlab.intake24.foodsql.admin.FoodDatabaseAdminImpl
import uk.ac.ncl.openlab.intake24.foodsql.tools.XmlImporter
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll

import org.scalatest.ConfigMap
import org.scalatest.Spec
import org.scalatest.SequentialNestedSuiteExecution
import org.scalatest.Suite

class FoodDatabaseAdminTest extends Spec with SequentialNestedSuiteExecution with BeforeAndAfterAll with TestFoodDatabase {

  override def beforeAll(configMap: ConfigMap) {
    resetTestDatabase()
  }

  override def afterAll(configMap: ConfigMap) {
    testDataSource.close()
  }

  override def nestedSuites() = {
    val service = new FoodDatabaseAdminImpl(testDataSource)

    Vector(
        //new LocalesAdminSuite(service),
        //new FoodGroupsAdminSuite(service),
        new CategoriesAdminSuite(service)
        //new AsServedImageAdminSuite(service),
        //new AssociatedFoodsAdminSuite(service),
        //new BrandNamesAdminSuite(service)
        )
  }
}
