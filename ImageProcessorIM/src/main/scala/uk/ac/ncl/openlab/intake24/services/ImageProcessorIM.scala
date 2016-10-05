package uk.ac.ncl.openlab.intake24.services.fooddb.images

import javax.inject.Inject
import java.io.File
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import scala.collection.mutable.Buffer

class ImageProcessorIM @Inject() (val settings: ImageProcessorSettings) extends ImageProcessor {

  def processForAsServed(sourceImages: Seq[File]): Either[ImageProcessorError, Seq[ProcessedAsServedImage]] = {
    try {

      val result = Buffer[ProcessedAsServedImage]()

      sourceImages.foreach {
        sourceImage =>

          val cmd = new ConvertCmd()
          val op = new IMOperation()

          op.resize(settings.asServed.width)
          op.background("white")
          op.gravity("Center")
          op.extent(settings.asServed.width, settings.asServed.height)
          op.addImage(sourceImage.getAbsolutePath())

          val mainImage = File.createTempFile("", s"_${sourceImage.getName}")

          op.addImage(mainImage.getAbsolutePath)

          cmd.run(op)

          val op2 = new IMOperation()
          op2.resize((settings.asServed.thumbnailWidth / 0.7).toInt)
          op2.gravity("Center");
          op2.addRawArgs("-crop", "70%x80%+0+0")
          op2.addImage(sourceImage.getAbsolutePath())

          val thumbnail = File.createTempFile("", s"_thumb_${sourceImage.getName}")

          op2.addImage(thumbnail.getAbsolutePath)
          cmd.run(op2)

          result.append(ProcessedAsServedImage(mainImage, thumbnail))
      }

      Right(result.toSeq)

    } catch {
      case e: Throwable => Left(IOError(e))
    }

  }

  def processForGuideImageBase(sourceImage: File): Either[ImageProcessorError, File] = ???
  def processForGuideImageOverlays(sourceImage: File): Either[ImageProcessorError, Map[Int, File]] = ???
}