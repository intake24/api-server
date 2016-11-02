package uk.ac.ncl.openlab.intake24.services.fooddb.images

import javax.inject.Inject
import java.io.File
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import scala.collection.mutable.Buffer
import java.nio.file.Path
import javax.inject.Singleton
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._

@Singleton
class ImageProcessorIM @Inject() (val settings: ImageProcessorSettings) extends ImageProcessor {
  
  val logger = LoggerFactory.getLogger(classOf[ImageProcessorIM])

  def processForAsServed(sourceImage: Path, mainImageDest: Path, thumbnailDest: Path): Either[ImageProcessorError, Unit] = {
    try {
      val cmd = new ConvertCmd()
      val op = new IMOperation()

      op.resize(settings.asServed.width)
      op.background("white")
      op.gravity("Center")
      op.extent(settings.asServed.width, settings.asServed.height)
      op.addImage(sourceImage.toString())
      op.addImage(mainImageDest.toString())
      
      logger.debug(s"Invoking ImageMagick for main image: ${((cmd.getCommand.asScala) ++ (op.getCmdArgs.asScala)).mkString(" ")}")
      
      cmd.run(op)

      val op2 = new IMOperation()
      op2.resize((settings.asServed.thumbnailWidth / 0.7).toInt)
      op2.gravity("Center");
      op2.addRawArgs("-crop", "70%x80%+0+0")
      op2.addImage(sourceImage.toString())
      op2.addImage(thumbnailDest.toString())
      
      logger.debug(s"Invoking ImageMagick for thumbnail: ${((cmd.getCommand.asScala) ++ (op2.getCmdArgs.asScala)).mkString(" ")}")
      
      cmd.run(op2)

      Right(())

    } catch {
      case e: Throwable => Left(ImageProcessorError(e))
    }
  }

  def processForGuideImageBase(sourceImage: Path, dest: Path): Either[ImageProcessorError, Unit] = ???
  def processForGuideImageOverlays(sourceImage: Path, destDir: Path): Either[ImageProcessorError, Map[Int, Path]] = ???

  def processForSelectionScreen(sourceImage: Path, dest: Path): Either[ImageProcessorError, Unit] = { 
    try {
      val cmd = new ConvertCmd()
      val op = new IMOperation()

      op.resize(settings.selection.width)
      op.background("white")
      op.gravity("Center")
      op.extent(settings.selection.width, settings.selection.height)
      op.addImage(sourceImage.toString())
      op.addImage(dest.toString())
      
      logger.debug(s"Invoking ImageMagick for selection screen image: ${((cmd.getCommand.asScala) ++ (op.getCmdArgs.asScala)).mkString(" ")}")
      
      cmd.run(op)

      Right(())

    } catch {
      case e: Throwable => Left(ImageProcessorError(e))
    }   
  }
}