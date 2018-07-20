package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import uk.ac.ncl.openlab.intake24.sql.tools._

object FoodV18_2_Create_ImageMaps extends App with MigrationRunner with WarningMessage {
/*
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

  val signinService = new SigninClientImpl(apiConfig.baseUrl)
  val imageMapAdminService = new ImageMapAdminClientImpl(apiConfig.baseUrl)

  println("Loading legacy image map descriptions")

  val descriptions = read[FoodV18_Guide_Descriptions](scala.io.Source.fromFile(options.descFile()).getLines().mkString)

  println("Signin in to the API server")

  signinService.signin(EmailCredentials(apiConfig.userName, apiConfig.password)) match {
    case Right(SigninResult(token)) => {

      val names = descriptions.legacyImageMapList.sorted

      val existing = imageMapAdminService.listImageMaps(token) match {
        case Right(headers) => headers
        case Left(x) => throw new RuntimeException(x.toString)
      }

      val objectDescriptions = (descriptions.objectDescriptions
        + ("Ghotdrinks" -> Range(1, 8).map((_, "Hot drink takeaway cup")))
        + ("Gmug" -> Range(1, 7).map((_, "Mug")))
        + ("gbeer" -> Seq(1, 7, 3, 2).map((_, "Beer glass")))
        + ("gbowl" -> Range(1, 7).map((_, "Bowl")))
        + ("gcolddrinks" -> Range(8, 14).map((_, "Cold drink takeaway cup")))
        + ("gfries" -> Range(1, 4).map((_, "Fries")))
        + ("gpiz1" -> Range(1, 4).map((_, "Pizza slice")))
        + ("gpiz2" -> Range(1, 4).map((_, "Pizza slice")))
        + ("gpiz3" -> Range(1, 4).map((_, "Pizza slice")))
        + ("gpiz4" -> Range(1, 4).map((_, "Pizza slice")))
        + ("gpiz5" -> Range(1, 4).map((_, "Pizza slice")))
        + ("gpiz6" -> Range(1, 3).map((_, "Pizza slice")))
        + ("gpiz7" -> Range(1, 4).map((_, "Pizza slice")))
        + ("gpiz8" -> Range(1, 4).map((_, "Pizza slice")))
        + ("gpiz9" -> Range(1, 4).map((_, "Pizza slice")))
        + ("gpizza" -> Range(1, 11).map((_, "Pizza")))
        + ("gpthick" -> Range(1, 6).map((_, "Pizza thickness")))
        + ("gsand" -> Range(1, 4).map((_, "Sandwich")))
        + ("gsmsau" -> Range(1, 4).map((_, "Smoked sausage")))
        + ("gsoftdrnk" -> Range(1, 8).map((_, "Glass")))
        + ("gspirits" -> Seq(3, 4, 5, 6, 11, 12).map((_, "Glass")))
        + ("gwine" -> Seq(8, 9, 10).map((_, "Wine glass")))
        + ("milkbowlA" -> Range(1, 7).map((_, "Milk level")))
        + ("milkbowlB" -> Range(1, 5).map((_, "Milk level")))
        + ("milkbowlC" -> Range(1, 7).map((_, "Milk level")))
        + ("milkbowlD" -> Range(1, 7).map((_, "Milk level")))
        + ("milkbowlE" -> Range(1, 7).map((_, "Milk level")))
        + ("milkbowlF" -> Range(1, 7).map((_, "Milk level")))
        )

      names.foreach {
        name =>
          println()
          println(s"=== Processing $name ===")

          if (existing.exists(_.id == name)) {
            println("Image map already exists, skipping")
          } else {

            val baseImageSourcePath = findBaseImageSource(name)
            val svgPath = findSVG(name)

            println(s"Using ${baseImageSourcePath.toString} as base image source")
            println(s"Using ${svgPath.toString} as SVG image map")

            val request = NewImageMapRequest(name, "No description", objectDescriptions(name).map(x => (x._1.toString, x._2)).toMap)

            println("Sending API request to create the image map")

            imageMapAdminService.createImageMap(token, baseImageSourcePath, svgPath, List(), request) match {
              case Left(ErrorParseFailed(code, e)) => throw new RuntimeException(s"API request failed with HTTP error $code", e)
              case Left(RequestFailed(_, cause, _)) if cause == "DuplicateCode" => println(s"Image map creation failed due to duplicate code, skipping $name")
              case Left(RequestFailed(code, cause, errorMessage)) => throw new RuntimeException(s"API request failed: HTTP error $code: $cause: $errorMessage")
              case Right(()) => ()
            }
          }
      }
    }
    case Left(x)
    => throw new RuntimeException("Sign in failed: " + x.toString)
  }
*/
}