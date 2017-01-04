package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import java.nio.file.{Files, Path, Paths}
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate

import org.apache.commons.io.FilenameUtils
import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.client.ApiError.{ErrorParseFailed, RequestFailed}
import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.{ImageMapAdminImpl, SigninImpl}
import uk.ac.ncl.openlab.intake24.api.client.{ApiConfigChooser, ApiConfigurationOptions}
import uk.ac.ncl.openlab.intake24.api.shared.{AuthToken, Credentials, NewImageMapRequest}
import uk.ac.ncl.openlab.intake24.services.fooddb.images.SVGImageMapParser
import uk.ac.ncl.openlab.intake24.sql.tools._
import upickle.default._

import scala.concurrent.duration._
import scala.concurrent.Await

object FoodV18_2_Create_ImageMaps extends App with MigrationRunner with WarningMessage {

  val svgParser = new SVGImageMapParser()

  trait Options extends ScallopConf with ApiConfigurationOptions {

    val sourceImageDir = opt[String](required = true, noshort = true)
    val imageDir = opt[String](required = true, noshort = true)
    val svgDir = opt[String](required = true, noshort = true)
    val descFile = opt[String](required = true, noshort = true)
  }

  val options = new ScallopConf(args) with Options

  options.verify()

  private def findBaseImageSource(id: String): Path = {

    println(s"Trying to locate source file for $id")

    val fileName = s"$id.jpg"

    val matcher = new BiPredicate[Path, BasicFileAttributes] {
      def test(path: Path, attr: BasicFileAttributes) = path.getFileName().toString().equals(fileName)
    }

    val hiResSource = Files.find(Paths.get(options.sourceImageDir()), 20, matcher).findFirst()

    if (hiResSource.isPresent())
      hiResSource.get()
    else {
      println(s"No high-res source found for $id")
      val lowResSource = Files.find(Paths.get(options.imageDir()), 20, matcher).findFirst()
      if (lowResSource.isPresent())
        lowResSource.get()
      else
        throw new RuntimeException(s"Unable to locate source image for $id")
    }
  }

  private def findSVG(id: String): Path = {

    println(s"Trying to locate source SVG for $id")

    val fileName = s"$id.svg"

    val predicate = new BiPredicate[Path, BasicFileAttributes] {
      override def test(t: Path, u: BasicFileAttributes): Boolean = t.getFileName.toString == fileName
    }

    val svgOption = Files.find(Paths.get(options.svgDir()), 10, predicate).findFirst()

    if (svgOption.isPresent)
      svgOption.get()
    else
      throw new RuntimeException(s"Unable to locate image map SVG for $id")
  }


  val apiConfig = ApiConfigChooser.chooseApiConfiguration(configDirPath = options.apiConfigDir())

  val signinService = new SigninImpl(apiConfig.baseUrl)
  val imageMapAdminService = new ImageMapAdminImpl(apiConfig.baseUrl)

  println("Loading legacy image map descriptions")

  val descriptions = read[FoodV18_2_Guide_Descriptions](scala.io.Source.fromFile(options.descFile()).getLines().mkString)

  println("Signin in to the API server")

  signinService.signin(Credentials("", apiConfig.userName, apiConfig.password)) match {
    case Right(AuthToken(token)) => {

      val names = descriptions.legacyImageMapList.sorted

      names.foreach {
        name =>
          println()
          println(s"=== Processing $name ===")

          val baseImageSourcePath = findBaseImageSource(name)
          val svgPath = findSVG(name)

          println(s"Using ${baseImageSourcePath.toString} as base image source")
          println(s"Using ${svgPath.toString} as SVG image map")

          val request = NewImageMapRequest(name, "No description", descriptions.objectDescriptions(name).map(x => (x._1.toString, x._2)).toMap)

          println("Sending API request to create the image map")

          imageMapAdminService.createImageMap(token, baseImageSourcePath, svgPath, List(), request) match {
            case Left(ErrorParseFailed(code, _)) => throw new RuntimeException(s"API request failed with HTTP error $code")
            case Left(RequestFailed(code, cause, errorMessage)) => throw new RuntimeException(s"API request failed: $cause: $errorMessage")
            case Right(()) => ()
          }
      }
    }
    case Left(x) => throw new RuntimeException("Sign in failed: " + x.toString)
  }

}