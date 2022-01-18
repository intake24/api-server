import com.google.inject.Inject
import com.typesafe.config.{ConfigFactory, ConfigParseOptions, ConfigResolveOptions}
import modules.PairwiseAssociationsTestModule
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import play.api.db.{DBModule, Database, HikariCPModule, NamedDatabase}
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.inject.{ApplicationLifecycle, DefaultApplicationLifecycle, bind}
import play.api.{Configuration, Environment}
import uk.ac.ncl.openlab.intake24.api.data.NewUserProfile
import uk.ac.ncl.openlab.intake24.errors.AnyError
import uk.ac.ncl.openlab.intake24.services.systemdb.Roles
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.{SurveyAdminService, SurveyParametersIn, UserAdminService}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.PairwiseAssociationsService
import uk.ac.ncl.openlab.intake24.services.systemdb.user.{ErrorReportingSettings, SurveyService}
import uk.ac.ncl.openlab.intake24.surveydata._

import java.time.ZonedDateTime
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

case class TestDatabases @Inject()(@NamedDatabase("intake24_system") system: Database, @NamedDatabase("intake24_foods") foods: Database)

class PairwiseAssociationsTests extends FunSuite with BeforeAndAfterEach with DatabaseUtils {

  val config = ConfigFactory.load("application-test.conf", ConfigParseOptions.defaults().setAllowMissing(false), ConfigResolveOptions.defaults())

  val configuration = Configuration(config)

  val injector = new GuiceInjectorBuilder()
    .configure(configuration)
    .bindings(
      new HikariCPModule,
      new DBModule,
      new PairwiseAssociationsTestModule,
      bind[Configuration].toInstance(configuration),
      bind[Environment].toInstance(Environment.simple()),
      bind[ApplicationLifecycle].to[DefaultApplicationLifecycle])
    .build()


  var paService: PairwiseAssociationsService = null
  var surveyAdminService: SurveyAdminService = null
  var surveyService: SurveyService = null
  var userAdminService: UserAdminService = null

  var userIds: Seq[Long] = null

  val databases = injector.instanceOf[TestDatabases]

  private def assertSuccessful(result: Either[AnyError, Any]) = result match {
    case Left(error) => fail(error.exception)
    case Right(_) => ()
  }

  private def assertFailed(result: Either[AnyError, Any]) = result match {
    case Left(_) => ()
    case Right(_) => fail("Expected operation to fail")
  }

  override def beforeEach() = {
    ensureEmpty(databases.system)
    initSystem(databases.system)

    surveyAdminService = injector.instanceOf[SurveyAdminService]
    surveyService = injector.instanceOf[SurveyService]
    userAdminService = injector.instanceOf[UserAdminService]

    val createSurveyResult = surveyAdminService.createSurvey(SurveyParametersIn(
      "test1",
      "default",
      "en_GB",
      2,
      ZonedDateTime.now().minusYears(1),
      ZonedDateTime.now().plusYears(1),
      true,
      None,
      None,
      "test@test.test",
      None,
      None,
      None,
      false,
      1,
      None,
      1, None, 0, None, ErrorReportingSettings(true, true), "paRules", 20
    ))

    createSurveyResult match {
      case Left(error) => throw error.exception
      case Right(_) => ()
    }

    val createUsersResult = userAdminService.createUsers(Seq(NewUserProfile(None, None, None, Set(Roles.surveyRespondent("test1")), Map())))

    createUsersResult match {
      case Left(error) => throw error.exception
      case Right(ids) => userIds = ids
    }
  }

  override def afterEach(): Unit = {
    cleanup(databases.system)
  }

  private def createSubmission(surveyId: String, foodCodes: Seq[Seq[String]]): Unit = {
    val meals = foodCodes.map {
      foods =>
        NutrientMappedMeal(
          "Breakfast",
          MealTime(10, 0),
          Map(),

          foods.map {
            foodCode =>
              NutrientMappedFood(
                foodCode,
                "Food",
                "Food",
                false,
                "food",
                "Brand",
                PortionSizeWithWeights(100, 0, 100, "method", Map("servingWeight" -> "100.0")),
                Map(),
                Some("NTID"),
                Some("1234"),
                true,
                1,
                "Group",
                None,
                Map(),
                Map()
              )
          },
          Seq()
        )
    }

    val submisson = NutrientMappedSubmission(
      ZonedDateTime.now().minusMinutes(30),
      ZonedDateTime.now(),
      UUID.randomUUID(),
      meals,
      Map()
    )

    assertSuccessful(surveyService.createSubmission(userIds(0), surveyId, submisson))
  }

  test("Simple occurrence and co-occurrence count") {

    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))
    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))
    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))

    val paService = injector.instanceOf[PairwiseAssociationsService]

    assertSuccessful(Await.result(paService.rebuild(), 1 minute))

    val data = paService.getAssociationRules()("en_GB").getParams()

    assert(data.occurrences("F001") == 9)
    assert(data.occurrences("F002") == 9)
    assert(data.occurrences("F003") == 9)

    assert(data.coOccurrences("F001")("F002") == 9)
    assert(data.coOccurrences("F002")("F001") == 9)
    assert(data.coOccurrences("F003")("F002") == 9)
  }

  test("Rebuild should not change occurrences/co-occurrences counts") {

    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))
    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))
    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))

    val paService = injector.instanceOf[PairwiseAssociationsService]

    assertSuccessful(Await.result(paService.rebuild(), 1 minute))

    val data = paService.getAssociationRules()("en_GB").getParams()

    assert(data.occurrences("F001") == 9)
    assert(data.occurrences("F002") == 9)
    assert(data.occurrences("F003") == 9)

    assert(data.coOccurrences("F001")("F002") == 9)
    assert(data.coOccurrences("F002")("F001") == 9)
    assert(data.coOccurrences("F003")("F002") == 9)

    assertSuccessful(Await.result(paService.rebuild(), 1 minute))

    val dataAfterRebuild = paService.getAssociationRules()("en_GB").getParams()

    assert(data == dataAfterRebuild)
  }

  test("Rebuild should include new data") {

    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))

    val paService = injector.instanceOf[PairwiseAssociationsService]

    assertSuccessful(Await.result(paService.rebuild(), 1 minute))

    val data = paService.getAssociationRules()("en_GB").getParams()

    assert(data.occurrences("F001") == 3)
    assert(data.occurrences("F002") == 3)
    assert(data.occurrences("F003") == 3)

    assert(data.coOccurrences("F001")("F002") == 3)
    assert(data.coOccurrences("F002")("F001") == 3)
    assert(data.coOccurrences("F003")("F002") == 3)

    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))
    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))

    assertSuccessful(Await.result(paService.rebuild(), 1 minute))

    val dataAfterRebuild = paService.getAssociationRules()("en_GB").getParams()

    assert(dataAfterRebuild.occurrences("F001") == 9)
    assert(dataAfterRebuild.occurrences("F002") == 9)
    assert(dataAfterRebuild.occurrences("F003") == 9)

    assert(dataAfterRebuild.coOccurrences("F001")("F002") == 9)
    assert(dataAfterRebuild.coOccurrences("F002")("F001") == 9)
    assert(dataAfterRebuild.coOccurrences("F003")("F002") == 9)
  }

  test("Incremental update should include new data") {

    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))

    val paService = injector.instanceOf[PairwiseAssociationsService]

    assertSuccessful(Await.result(paService.rebuild(), 1 minute))

    val data = paService.getAssociationRules()("en_GB").getParams()

    assert(data.occurrences("F001") == 3)
    assert(data.occurrences("F002") == 3)
    assert(data.occurrences("F003") == 3)

    assert(data.coOccurrences("F001")("F002") == 3)
    assert(data.coOccurrences("F002")("F001") == 3)
    assert(data.coOccurrences("F003")("F002") == 3)

    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))
    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))

    assertSuccessful(Await.result(paService.update(), 1 minute))

    val dataAfterUpdate = paService.getAssociationRules()("en_GB").getParams()

    assert(dataAfterUpdate.occurrences("F001") == 9)
    assert(dataAfterUpdate.occurrences("F002") == 9)
    assert(dataAfterUpdate.occurrences("F003") == 9)

    assert(dataAfterUpdate.coOccurrences("F001")("F002") == 9)
    assert(dataAfterUpdate.coOccurrences("F002")("F001") == 9)
    assert(dataAfterUpdate.coOccurrences("F003")("F002") == 9)
  }

  test("Incremental update should be safe to re-run without new data") {

    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))

    val paService = injector.instanceOf[PairwiseAssociationsService]

    assertSuccessful(Await.result(paService.rebuild(), 1 minute))

    val data = paService.getAssociationRules()("en_GB").getParams()

    assert(data.occurrences("F001") == 3)
    assert(data.occurrences("F002") == 3)
    assert(data.occurrences("F003") == 3)

    assert(data.coOccurrences("F001")("F002") == 3)
    assert(data.coOccurrences("F002")("F001") == 3)
    assert(data.coOccurrences("F003")("F002") == 3)

    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))
    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))

    assertSuccessful(Await.result(paService.update(), 1 minute))

    val dataAfterUpdate = paService.getAssociationRules()("en_GB").getParams()

    assert(dataAfterUpdate.occurrences("F001") == 9)
    assert(dataAfterUpdate.occurrences("F002") == 9)
    assert(dataAfterUpdate.occurrences("F003") == 9)

    assert(dataAfterUpdate.coOccurrences("F001")("F002") == 9)
    assert(dataAfterUpdate.coOccurrences("F002")("F001") == 9)
    assert(dataAfterUpdate.coOccurrences("F003")("F002") == 9)

    assertSuccessful(Await.result(paService.update(), 1 minute))

    assertSuccessful(Await.result(paService.update(), 1 minute))

    val dataAfterMoreUpdates = paService.getAssociationRules()("en_GB").getParams()

    assert(dataAfterMoreUpdates.occurrences("F001") == 9)
    assert(dataAfterMoreUpdates.occurrences("F002") == 9)
    assert(dataAfterMoreUpdates.occurrences("F003") == 9)

    assert(dataAfterMoreUpdates.coOccurrences("F001")("F002") == 9)
    assert(dataAfterMoreUpdates.coOccurrences("F002")("F001") == 9)
    assert(dataAfterMoreUpdates.coOccurrences("F003")("F002") == 9)
  }

  test("Multiple concurrent updates should not be allowed")
  {
    createSubmission("test1", Seq(Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003"), Seq("F001", "F002", "F003")))

    val paService = injector.instanceOf[PairwiseAssociationsService]

    val shouldSucceed = paService.update()
    val shouldFail = paService.update()

    assertSuccessful(Await.result(shouldSucceed, 1 minute))
    assertFailed(Await.result(shouldFail, 1 minute))
    assertSuccessful(Await.result(paService.update(), 1 minute))
  }
}
