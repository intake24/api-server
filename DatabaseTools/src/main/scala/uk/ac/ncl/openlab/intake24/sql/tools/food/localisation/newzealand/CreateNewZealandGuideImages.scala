package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation.newzealand

import java.nio.file.Paths

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.client.{ApiConfigChooser, ApiConfigurationOptions, ConsoleApiErrorHandler, Intake24ApiClient}
import uk.ac.ncl.openlab.intake24.api.shared.{EmailCredentials, NewGuideImageRequest, NewImageMapRequest}

object CreateNewZealandPortionSizeMethods extends App with ConsoleApiErrorHandler {

  val options = new ScallopConf(args) with ApiConfigurationOptions

  options.verify()

  val apiConfig = ApiConfigChooser.chooseApiConfiguration(options.apiConfigDir())

  val apiClient = new Intake24ApiClient(apiConfig.baseUrl, EmailCredentials(apiConfig.userName, apiConfig.password))

  def descPlaceholders(numObjects: Int, placeholder: String): Map[String, String] =
    Range(1, numObjects + 1).map(id => (id.toString -> placeholder)).toMap

  val result = for (
    _ <- apiClient.imageMaps.createImageMap(
      Paths.get("/Users/nip13/Projects/Intake24/Misc/intake24-images-highres/GMusselpipi.jpg"),
      Paths.get("/Users/nip13/Projects/Intake24/intake24-data/GuideImages/Gmusselpipi.svg"),
      Seq("mussel", "pipi", "scallop", "clams"),
      NewImageMapRequest("Gmusselpipi", "Mussels and pipis (New Zealand)",
        descPlaceholders(7, "Mussel/pipi"))).right;

    _ <- apiClient.imageMaps.createImageMap(
      Paths.get("/Users/nip13/Projects/Intake24/Misc/intake24-images-highres/Gmuffscone.jpg"),
      Paths.get("/Users/nip13/Projects/Intake24/intake24-data/GuideImages/Gmuffscone.svg"),
      Seq("muffin", "scone"),
      NewImageMapRequest("Gmuffscone", "Muffins and scones",
        descPlaceholders(9, "Muffin/scone"))).right;

    _ <- apiClient.guideImages.createGuideImage(
      NewGuideImageRequest("Gmusselpipi", "Mussels and pipis (New Zealand)", "Gmusselpipi",
        Map("1" -> 8.0,
          "2" -> 4.0,
          "3" -> 2.0,
          "4" -> 26.0,
          "5" -> 24.0,
          "6" -> 18.0,
          "7" -> 12.0))).right;

    _ <- apiClient.guideImages.createGuideImage(
      NewGuideImageRequest("Gmuffscone", "Mussels and pipis (New Zealand)", "Gmusselpipi",
        Map("1" -> 122.0,
          "2" -> 68.0,
          "3" -> 32.0,
          "4" -> 154.0,
          "5" -> 68.0,
          "6" -> 120.0,
          "7" -> 60.0,
          "8" -> 134.0,
          "9" -> 94.0))).right) yield ()

  checkApiError(result)
}
