package uk.ac.ncl.openlab.intake24.sql.tools.food.localisation.newzealand

import java.nio.file.Paths

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.client.ApiError.{ErrorParseFailed, RequestFailed, ResultParseFailed}
import uk.ac.ncl.openlab.intake24.api.client.{ApiConfigChooser, ApiConfigurationOptions, Intake24ApiClient}
import uk.ac.ncl.openlab.intake24.api.shared.{EmailCredentials, NewImageMapRequest}
import uk.ac.ncl.openlab.intake24.errors.ErrorUtils

object CreateNewZealandPortionSizeMethods extends App {

  val options = new ScallopConf(args) with ApiConfigurationOptions

  options.verify()

  val apiConfig = ApiConfigChooser.chooseApiConfiguration(options.apiConfigDir())

  val apiClient = new Intake24ApiClient(apiConfig.baseUrl, EmailCredentials(apiConfig.userName, apiConfig.password))

  def descPlaceholders(numObjects: Int, placeholder: String): Map[String, String] =
    Range(1, numObjects + 1).map(id => (id.toString -> placeholder)).toMap

  val map1 = apiClient.imageMaps.createImageMap(
    Paths.get("/Users/nip13/Projects/Intake24/Misc/intake24-images-highres/GMusselpipi.jpg"),
    Paths.get("/Users/nip13/Projects/Intake24/intake24-data/GuideImages/Gmusselpipi.svg"),
    Seq("mussel", "pipi", "scallop", "clams"),
    NewImageMapRequest("Gmusselpipi", "Mussels and pipis (New Zealand)",
      descPlaceholders(7, "Mussel/pipi")))

  val map2 = apiClient.imageMaps.createImageMap(
    Paths.get("/Users/nip13/Projects/Intake24/Misc/intake24-images-highres/Gmuffscone.jpg"),
    Paths.get("/Users/nip13/Projects/Intake24/intake24-data/GuideImages/Gmuffscone.svg"),
    Seq("mussel", "pipi", "scallop", "clams"),
    NewImageMapRequest("Gmuffscone", "Mussels and pipis (New Zealand)",
      descPlaceholders(9, "Muffin/scone")))

  ErrorUtils.sequence(Seq(map1, map2)) match {
    case Right(_) => println("OK")
    case Left(RequestFailed(code: Int, errorCode, errorMessage)) => println(s"HTTP $code: $errorCode: $errorMessage")
    case Left(ResultParseFailed(e)) => throw e
    case Left(ErrorParseFailed(_, cause: Throwable)) => throw cause
  }

}
