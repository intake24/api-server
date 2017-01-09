package uk.ac.ncl.openlab.intake24.sql.tools.food.migrations

import org.rogach.scallop.ScallopConf
import uk.ac.ncl.openlab.intake24.api.client.scalajhttp.{GuideImageAdminClientImpl, ImageAdminClientImpl, ImageMapAdminClientImpl, SigninClientImpl}
import uk.ac.ncl.openlab.intake24.api.client.{ApiConfigChooser, ApiConfigurationOptions, ConsoleApiErrorHandler}
import uk.ac.ncl.openlab.intake24.api.shared.Credentials
import uk.ac.ncl.openlab.intake24.sql.tools._

object FoodV18_3_Create_Selection_Images extends App with MigrationRunner with WarningMessage with ConsoleApiErrorHandler {

  trait Options extends ScallopConf with ApiConfigurationOptions

  val options = new ScallopConf(args) with Options

  options.verify()

  val apiConfig = ApiConfigChooser.chooseApiConfiguration(configDirPath = options.apiConfigDir())

  val signinService = new SigninClientImpl(apiConfig.baseUrl)

  val imageMapAdminService = new ImageMapAdminClientImpl(apiConfig.baseUrl)
  val imageAdminService = new ImageAdminClientImpl(apiConfig.baseUrl)
  val guideImageAdminService = new GuideImageAdminClientImpl(apiConfig.baseUrl)

  def sequence[A, B](s: Seq[Either[A, B]]): Either[A, Seq[B]] =
    s.foldRight(Right(Nil): Either[A, List[B]]) {
      (e, acc) => for (xs <- acc.right; x <- e.right) yield x :: xs
    }

  def updateSelectionScreenImage(authToken: String, id: String) =
    for (
      _ <- {
        println(s"Processing $id");
        Right(())
      }.right;
      sourceId <- imageMapAdminService.getImageMapBaseImageSourceId(authToken, id).right;
      selectionImageDescriptor <- imageAdminService.processForSelectionScreen(authToken, s"guide/$id/selection", sourceId).right;
      _ <- guideImageAdminService.updateGuideSelectionImage(authToken, id, selectionImageDescriptor.id).right) yield ()

  val result = for (
    authToken <- signinService.signin(Credentials("", apiConfig.userName, apiConfig.password)).right;
    guideHeaders <- guideImageAdminService.listGuideImages(authToken.token).right;
    result <- sequence(guideHeaders.map {
      header =>
        updateSelectionScreenImage(authToken.token, header.id)
    }).right.map(_ => ()).right
  ) yield result

  checkApiError(result)
}