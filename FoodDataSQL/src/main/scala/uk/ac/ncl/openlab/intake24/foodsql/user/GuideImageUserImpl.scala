package uk.ac.ncl.openlab.intake24.foodsql.user

import scala.Left
import scala.Right

import anorm.Macro
import anorm.NamedParameter.symbol
import anorm.SQL
import anorm.sqlToSimple
import uk.ac.ncl.openlab.intake24.GuideImage
import uk.ac.ncl.openlab.intake24.GuideImageWeightRecord

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.RecordNotFound
import uk.ac.ncl.openlab.intake24.services.fooddb.user.GuideImageService
import uk.ac.ncl.openlab.intake24.foodsql.FoodDataSqlService
import uk.ac.ncl.openlab.intake24.sql.SqlResourceLoader

trait GuideImageUserImpl extends GuideImageService with FoodDataSqlService with SqlResourceLoader {

  private case class GuideResultRow(base_image_path: String, selection_image_path: String,
      image_description: String, object_id: Array[Long], object_description: Array[String], object_weight: Array[Double])
      
  private lazy val guideImageQuery = sqlFromResource("user/get_guide_image.sql")

  def getGuideImage(id: String): Either[LookupError, GuideImage] = tryWithConnection {
    implicit conn =>

      val result = SQL(guideImageQuery).on('id -> id).executeQuery().as(Macro.namedParser[GuideResultRow].singleOpt)
      
      result match {
        case None => Left(RecordNotFound(new RuntimeException(id)))
        case Some(row) =>
        val weights = result.map(row => GuideImageWeightRecord(row.object_description, row.object_id, row.weight))
        Right(GuideImage(id, result.head.image_description, weights))
      }
      
  }
}
