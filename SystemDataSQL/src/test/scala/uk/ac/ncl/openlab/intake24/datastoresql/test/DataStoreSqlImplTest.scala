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

package uk.ac.ncl.openlab.intake24.datastoresql.test

import org.scalatest.FunSuite
import uk.ac.ncl.openlab.intake24.datastoresql.DataStoreSqlImpl
import uk.ac.ncl.openlab.intake24.datastoresql.Util._
import java.time.LocalDate
import java.time.LocalDateTime
import java.sql.Timestamp
import scala.collection.JavaConversions._
import scala.util.Random
import java.util.UUID
import scala.collection.mutable.Buffer
import uk.ac.ncl.openlab.intake24.datastoresql.CompletedPortionSize
import uk.ac.ncl.openlab.intake24.datastoresql.NutritionMappedFood
import uk.ac.ncl.openlab.intake24.datastoresql.NutritionMappedMeal
import uk.ac.ncl.openlab.intake24.datastoresql.NutritionMappedSurvey
import uk.ac.ncl.openlab.intake24.datastoresql.NutritionMappedSurveyRecord
import uk.ac.ncl.openlab.intake24.datastoresql.MealTime
import net.scran24.datastore.shared.SurveyState
import uk.ac.ncl.openlab.intake24.datastoresql.SurveyParameters

import net.scran24.datastore.DataStoreException
import uk.ac.ncl.openlab.intake24.datastoresql.NutritionMappedSurveyRecordWithId
import java.sql.DriverManager
import org.postgresql.ds.PGSimpleDataSource
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SecureUserRecord

class DataStoreSqlImplTest extends FunSuite with TestDB {

  DriverManager.registerDriver(new org.postgresql.Driver)

  val dataSource = new PGSimpleDataSource()

  dataSource.setServerName("localhost");
  dataSource.setDatabaseName("intake24_system_test");
  dataSource.setUser("postgres");

  val service = new DataStoreSqlImpl(dataSource)

  def randomString() = Seq.fill(16)((Random.nextInt(26) + 97).toChar).mkString

  def randomFoodCode() = Seq.fill(4)((Random.nextInt(26) + 65).toChar).mkString

  def randomCustomFields(): Map[String, String] = {

    val map = scala.collection.mutable.Map[String, String]()

    (1 to (Random.nextInt(5) + 1)).foreach {
      index =>
        map += (randomString() -> randomString())
    }

    map.toMap
  }

  def randomNutrients(): Map[Long, Double] = Seq(1l,2l,3l,4l,5l).map(n => (n, Random.nextDouble() * 1000)).toMap

  def randomPortionSize() =
    new CompletedPortionSize(randomString(), randomCustomFields())

  def randomFoods() =
    Seq.fill(Random.nextInt(5) + 1) {
      new NutritionMappedFood(randomFoodCode(), randomString(), Some(randomString()), randomString(), randomString(), Random.nextBoolean(), randomString(), randomPortionSize(),
        Random.nextInt(), randomString(), Some(randomString()), Random.nextBoolean(), randomString(), randomNutrients(), randomCustomFields())
    }

  def randomMeals() =
    Seq.fill(Random.nextInt(5) + 1) {
      new NutritionMappedMeal(randomString(), randomFoods(), new MealTime(Random.nextInt(24), Random.nextInt(60)), randomCustomFields())
    }.toList

  def randomLog() = Seq.fill(Random.nextInt(10))(randomString())

  def randomSurvey() =
    new NutritionMappedSurvey(
      System.currentTimeMillis(),
      Random.nextInt(10000000) + System.currentTimeMillis(),
      randomMeals(),
      randomLog(),
      Seq("user1", "user2", "user3")(Random.nextInt(3)),
      randomCustomFields())

  def randomSubmissionRecord() =
    new NutritionMappedSurveyRecord(
      randomSurvey(),
      randomCustomFields())

  test("Create a survey") {
    service.initSurvey("test1", "test_scheme", "en_GB", true, Some("Test URL"))
  }

  test("Verify initial parameters") {

    val params = service.getSurveyParameters("test1")

    assert(params.state === SurveyState.NOT_INITIALISED.ordinal())
    assert(params.schemeName === "test_scheme")
    assert(params.allowGenUsers === true)
    assert(params.surveyMonkeyUrl === Some("Test URL"))
  }

  test("Create another survey") {
    service.initSurvey("test2", "test_scheme", "en_GB", true, None)
  }

  test("Verify initial parameters for the other survey") {
    val params = service.getSurveyParameters("test2")

    assert(params.state === SurveyState.NOT_INITIALISED.ordinal())
    assert(params.schemeName === "test_scheme")
    assert(params.allowGenUsers === true)
    assert(params.surveyMonkeyUrl === None)

  }

  test("Change survey parameters") {
    service.setSurveyParameters("test2", new SurveyParameters(SurveyState.ACTIVE.ordinal(),
      Timestamp.valueOf(LocalDateTime.of(2015, 8, 1, 0, 0)).getTime,
      Timestamp.valueOf(LocalDateTime.of(2015, 10, 30, 15, 30)).getTime, "test_scheme_2", "en_GB", false, "just4fun", Some("test_url")))
  }

  test("Verify changed survey parameters") {
    val params2 = service.getSurveyParameters("test2")

    assert(params2.state === SurveyState.ACTIVE.ordinal())
    assert(params2.schemeName === "test_scheme_2")
    assert(params2.allowGenUsers === false)
    assert(params2.surveyMonkeyUrl === Some("test_url"))
  }

  val user1 = new SecureUserRecord("user1", "blah", "blah", "blah", Set("role1", "role2"), Set("permission1", "permission2"), Map[String, String]())
  val user2 = new SecureUserRecord("user2", "blah", "blah", "blah", Set("role3", "role4"), Set("permission3", "permission4"), Map[String, String]())
  val user3 = new SecureUserRecord("user3", "blah", "blah", "blah", Set("role1", "role3"), Set("permission1", "permission3"), Map[String, String]())

  test("Attempt to add user to an unknown survey (throws an exception)") {
    val exception = intercept[DataStoreException] {
      service.addUser("no_such_survey", user1)
    }

    assert(exception.getCause.getMessage.startsWith("""ERROR: insert or update on table "users" violates foreign key constraint "users_surveys_id_fk""""))
  }

  test("Add some users") {
    service.addUser("test1", user1)
    service.addUser("test1", user2)
    service.addUser("test1", user3)
  }

  test("Retrieve all users for a survey") {
    val returned: List[SecureUserRecord] = service.getUserRecords("test1").toList
    assert(returned === List(user1, user2, user3))
  }

  test("Attempt to retrieve users for an unknown survey (returns an empty set)") {
    assert(service.getUserRecords("no_such_survey").isEmpty())
  }

  test("Retrieve users by role") {
    val returnedUsers1: List[SecureUserRecord] = service.getUserRecords("test1", "role1").toList
    assert(returnedUsers1 === List(user1, user3))

    val returnedUsers2: List[SecureUserRecord] = service.getUserRecords("test1", "role3").toList
    assert(returnedUsers2 === List(user2, user3))
  }

  test("Attempt to retrieve users with inexisting role (returns an empty set)") {
    assert(service.getUserRecords("test1", "no_such_role").isEmpty())
  }

  val randomSurvey1 = randomSubmissionRecord()

  val randomSurvey2 = randomSubmissionRecord()

  test("Submit some surveys") {
    service.saveSurvey("test1", "user1", randomSurvey1)
    service.saveSurvey("test1", "user2", randomSurvey2)
  }

  test("Retrieve surveys") {
    val buf = Buffer[NutritionMappedSurveyRecordWithId]()

    service.processSurveys("test1", System.currentTimeMillis() - 50000000, System.currentTimeMillis() + 50000000, r => buf += r)

    assert(buf.toList.map(r => NutritionMappedSurveyRecord(r.survey, r.userCustomFields)) === List(randomSurvey1, randomSurvey2))
  }

  /*  test("Performance test") {

    val t0 = System.currentTimeMillis()

    val manyRandomSurveys = (1 to 1000).foreach {
      index =>
        service.saveSurvey("test1", "user1", randomSubmissionRecord())
    }

    println("Uploaded 1000 surveys in " + (System.currentTimeMillis() - t0) + " ms")
    
    val t1 = System.currentTimeMillis()
    
     val buf = Buffer[NutritionMappedSurveyRecord]()
    
      service.processSurveys("test1", System.currentTimeMillis() - 50000000, System.currentTimeMillis() + 50000000, new Callback1[NutritionMappedSurveyRecord] {
      def call(r: NutritionMappedSurveyRecord) = {
        buf += r
      }
    })
    
    println("Processed 1000 surveys in " + (System.currentTimeMillis() - t1) + " ms")

  } */

  test("Update empty popularity counters") {
    service.incrementPopularityCount(Seq("AAAA", "BBBB", "CCCC"))
  }

  test("Update existing popularity counters") {
    service.incrementPopularityCount(Seq("AAAA", "BBBB", "CCCC"))
  }

  test("Update mixed popularity counters") {
    service.incrementPopularityCount(Seq("AAAA", "BBBB", "DDDD"))
  }

  test("Set new global value") {
    service.setGlobalValue("test1", "test")

    assert(service.getGlobalValue("test1") === Some("test"))
  }

  test("Update popularity counters with an empty list") {
    service.incrementPopularityCount(Seq())
  }

  test("Get popularity counters with an empty list") {
    service.getPopularityCount(Seq())
  }

  test("Set existing global value") {
    service.setGlobalValue("test1", "test123")

    assert(service.getGlobalValue("test1") === Some("test123"))
  }

  test("Get non-existing global value (returns None)") {
    assert(service.getGlobalValue("no_such_value") === None)
  }

  val t = System.currentTimeMillis()

  test("Set last help request time") {
    service.setLastHelpRequestTime("test1", "user1", t)
  }

  test("Get last help request time") {
    assert(service.getLastHelpRequestTime("test1", "user1") === Some(t))
  }

  test("Get last help request time for non-existing user") {
    assert(service.getLastHelpRequestTime("test1", "no_such_user") === None)
  }

}