import com.typesafe.config.{ConfigFactory, ConfigParseOptions, ConfigResolveOptions}
import modules.PairwiseAssociationsTestModule
import org.scalatest.FunSuite
import play.api.db.{DBModule, HikariCPModule}
import play.api.inject.guice.GuiceInjectorBuilder
import play.api.inject.{ApplicationLifecycle, DefaultApplicationLifecycle, bind}
import play.api.{Configuration, Environment}
import uk.ac.ncl.openlab.intake24.services.systemdb.pairwiseAssociations.PairwiseAssociationsService

class PairwiseAssociationsTests extends FunSuite {

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

  val paService = injector.instanceOf[PairwiseAssociationsService]

  test("test") {

  }
}
