package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.nio.file.Paths

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.{GuideImageAdminClientImpl, ImageMapAdminClientImpl, SigninClientImpl}
import uk.ac.ncl.openlab.intake24.api.client.{ApiConfigChooser, ApiConfigurationOptions}
import uk.ac.ncl.openlab.intake24.api.shared.{EmailCredentials, NewGuideImageRequest, NewImageMapRequest, SurveyAliasCredentials}
import uk.ac.ncl.openlab.intake24.sql.tools._
import uk.ac.ncl.openlab.intake24.sql.tools.food.migrations.FoodV18_3_Create_Selection_Images.checkApiError

object FoodV21_Create_DK_Guide_Images extends App with MigrationRunner with WarningMessage {

  def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, List[B]]) {
      (e, acc) => for (xs <- acc.right; x <- e.right) yield x :: xs
    }

  case class ImageMapParams(baseImagePath: String, svgPath: String, keywords: Seq[String], request: NewImageMapRequest)

  trait Options extends ScallopConf with ApiConfigurationOptions {

    val baseDir = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  val apiConfig = ApiConfigChooser.chooseApiConfiguration(configDirPath = options.apiConfigDir())

  val signinClient = new SigninClientImpl(apiConfig.baseUrl)
  val imageMapAdminClient = new ImageMapAdminClientImpl(apiConfig.baseUrl)
  val guideImageAdminClient = new GuideImageAdminClientImpl(apiConfig.baseUrl)

  def descPlaceholders(numObjects: Int, placeholder: String): Map[String, String] =
    Range(1, numObjects + 1).map(id => (id.toString -> placeholder)).toMap

  val imageMapParams = Seq(
    ImageMapParams("guide danish herring/Gdk herring.JPG", "guide danish herring/Gdk herring.svg", Seq("fish", "herring"),
      NewImageMapRequest("danish_herring", "Danish herring", descPlaceholders(3, "Herring"))),

    ImageMapParams("guide danish french pastry/Gdk frenchpastry.JPG", "guide danish french pastry/Gdk frenchpastry.svg", Seq("pastry", "french"),
      NewImageMapRequest("danish_french_pastry", "Danish French pastry", descPlaceholders(5, "Pastry"))),

    ImageMapParams("guide danish crisp bread/Gdk crispbread.jpg", "guide danish crisp bread/Gdk crispbread.svg", Seq("bread", "crispbread", "danish"),
      NewImageMapRequest("danish_crispbread", "Danish crispbread", descPlaceholders(7, "Crispbread"))),

    ImageMapParams("guide danish  sweets/GDKsweets.JPG", "guide danish  sweets/GDKsweets.svg", Seq("sweets", "danish"),
      NewImageMapRequest("danish_sweets", "Danish sweets", descPlaceholders(39, "Sweet"))),

    ImageMapParams("guide choc bites/GChocbites.JPG", "guide choc bites/GChocbites.svg", Seq("chocolate", "bites"),
      NewImageMapRequest("choc_bites", "Chocolate bites", descPlaceholders(5, "Chocolate bite"))),

    ImageMapParams("guide cereal bars unwrapped/Gcerealbarsunwrapped.JPG", "guide cereal bars unwrapped/Gcerealbarsunwrapped.svg", Seq("cereal", "bars", "unwrapped"),
      NewImageMapRequest("cereal_bars_unwrapped", "Cereal bars (unwrapped)", descPlaceholders(10, "Cereal bar"))),

    ImageMapParams("guide cereal bars/GCerealbars.JPG", "guide cereal bars/GCerealbars.svg", Seq("cereal", "bars", "wrapped"),
      NewImageMapRequest("cereal_bars_wrapped", "Cereal bars (wrapped)", descPlaceholders(10, "Cereal bar"))),

    ImageMapParams("guide bhajis/GBhaji.JPG", "guide bhajis/GBhaji.svg", Seq("bhaji"),
      NewImageMapRequest("bhaji", "Bhaji", descPlaceholders(10, "Bhaji"))),

    ImageMapParams("guide -danish wienerbroed pastry/Gdkpastry.jpg", "guide -danish wienerbroed pastry/Gdkpastry.svg", Seq("pastry", "danish", "wienerbrod"),
      NewImageMapRequest("danish_wienerbroed", "Danish wienerbroed pastry", descPlaceholders(5, "Pastry"))),

    ImageMapParams("guide -danish toerkager pastry/IMG_1585_toerkager_pastry_unyellow.jpg", "guide -danish toerkager pastry/IMG_1585_toerkager_pastry_unyellow.svg", Seq("pastry", "danish", "toerkager"),
      NewImageMapRequest("danish_toerkager", "Danish toerkager pastry", descPlaceholders(7, "Pastry")))
  )

  val guideImageRequests = Seq(
    NewGuideImageRequest("Gdk_herring", "Danish herring", "danish_herring", Map(
      "1" -> 20.0, "2" -> 20.0, "3" -> 20.0
    )),
    NewGuideImageRequest("Gdk_french_pastry", "Danish French pastry", "danish_french_pastry", Map(
      "1" -> 70.0, "2" -> 70.0, "3" -> 60.0, "4" -> 60.0, "5" -> 70.0
    )),
    NewGuideImageRequest("Gdk_crispbread", "Danish crispbread", "danish_crispbread", Map(
      "1" -> 16.0, "2" -> 16.0, "3" -> 12.0, "4" -> 12.0, "5" -> 12.0, "6" -> 12.0, "7" -> 12.0
    )),
    NewGuideImageRequest("Gdk_sweets", "Danish sweets", "danish_sweets", Map(
      /* 10 g = 1 + 2 */
      "1" -> 10.0, "2" -> 10.0,

      /* 5 g = 3-5 and 9-37 */
      "3" -> 5.0, "4" -> 5.0, "5" -> 5.0,
      "9" -> 5.0, "10" -> 5.0, "11" -> 5.0, "12" -> 5.0, "13" -> 5.0, "14" -> 5.0, "15" -> 5.0,
      "16" -> 5.0, "17" -> 5.0, "18" -> 5.0, "19" -> 5.0, "20" -> 5.0, "21" -> 5.0, "22" -> 5.0, "23" -> 5.0,
      "24" -> 5.0, "25" -> 5.0, "26" -> 5.0, "27" -> 5.0, "28" -> 5.0, "29" -> 5.0, "30" -> 5.0, "31" -> 5.0,
      "32" -> 5.0, "33" -> 5.0, "34" -> 5.0, "35" -> 5.0, "36" -> 5.0, "37" -> 5.0,

      /* 4 g = 6-8 and 38-39 */
      "6" -> 4.0, "7" -> 4.0, "8" -> 4.0,
      "38" -> 4.0, "39" -> 4.0
    )),
    NewGuideImageRequest("Gchocbites", "Chocolate bites", "choc_bites", Map(
      "1" -> 75.0, "2" -> 39.0, "3" -> 12.0, "4" -> 13.0, "5" -> 13.0
    )),
    NewGuideImageRequest("Gcbar_unwrapped", "Cereal bars (unwrapped)", "cereal_bars_unwrapped", Map(
      "1" -> 37.0, "2" -> 45.0, "3" -> 50.0, "4" -> 16.6, "5" -> 12.5,
      "6" -> 25.0, "7" -> 22.0, "8" -> 28.0, "9" -> 12.5, "10" -> 21.0
    )),
    NewGuideImageRequest("Gcbar_wrapped", "Cereal bars (wrapped)", "cereal_bars_wrapped", Map(
      "1" -> 37.0, "2" -> 45.0, "3" -> 50.0, "4" -> 49.8, "5" -> 50.0,
      "6" -> 25.0, "7" -> 22.0, "8" -> 28.0, "9" -> 25.0, "10" -> 42.0
    )),
    NewGuideImageRequest("Gbhaji", "Bhaji", "bhaji", Map(
      "1" -> 58.0, "2" -> 72.0, "3" -> 20.0, "4" -> 21.0, "5" -> 29.0,
      "6" -> 62.0, "7" -> 28.0, "8" -> 34.0, "9" -> 28.0, "10" -> 19.0
    )),
    NewGuideImageRequest("Gdk_wienerbroed", "Danish wienerbroed pastry", "danish_wienerbroed", Map(
      "1" -> 80.0, "2" -> 80.0, "3" -> 80.0, "4" -> 80.0, "5" -> 75.0
    )),
    NewGuideImageRequest("Gdk_toerkager", "Danish toerkager pastry", "danish_toerkager", Map(
      "1" -> 70.0, "2" -> 40.0, "3" -> 50.0, "4" -> 70.0, "5" -> 50.0, "6" -> 50.0, "7" -> 50.0
    ))
  )

  val result = for (
    authToken <- signinClient.signin(EmailCredentials(apiConfig.userName, apiConfig.password)).right;
    existingImageMaps <- imageMapAdminClient.listImageMaps(authToken.refreshToken).right;
    existingGuideImages <- guideImageAdminClient.listGuideImages(authToken.refreshToken).right;

    _ <- sequence(imageMapParams.filterNot(params => existingImageMaps.exists(_.id == params.request.id)).map {
      params =>
        imageMapAdminClient.createImageMap(authToken.refreshToken, Paths.get(options.baseDir() + "/" + params.baseImagePath),
          Paths.get(options.baseDir() + "/" + params.svgPath), params.keywords, params.request)
    }).right;
    _ <- sequence(guideImageRequests.filterNot(req => existingGuideImages.exists(_.id == req.id)).map(guideImageAdminClient.createGuideImage(authToken.refreshToken, _))).right)
    yield ()

  checkApiError(result)
}