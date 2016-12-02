package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.awt.{BasicStroke, Color, RenderingHints}
import java.awt.image.BufferedImage
import java.nio.file.{Files, Path}
import java.util.UUID
import javax.imageio.ImageIO
import javax.inject.{Inject, Singleton}

import org.im4java.core.{ConvertCmd, IMOperation}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

@Singleton
class ImageProcessorIM @Inject()(val settings: ImageProcessorSettings) extends ImageProcessor {

  val logger = LoggerFactory.getLogger(classOf[ImageProcessorIM])

  override def processForSourceThumbnail(sourceImage: Path, thumbnail: Path): Either[ImageProcessorError, Unit] =
    try {
      val cmd = new ConvertCmd()
      val op = new IMOperation()

      op.resize(settings.source.thumbnailWidth)
      op.background("white")
      op.gravity("Center")
      op.extent(settings.source.thumbnailWidth, settings.source.thumbnailHeight)
      op.addImage(sourceImage.toString())
      op.addImage(thumbnail.toString())

      logger.debug(s"Invoking ImageMagick for source image thumbnail: ${((cmd.getCommand.asScala) ++ (op.getCmdArgs.asScala)).mkString(" ")}")

      cmd.run(op)

      Right(())

    } catch {
      case e: Throwable => Left(ImageProcessorError(e))
    }

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

  def generateImageMapOverlays(imageMap: AWTImageMap, directory: Path): Either[ImageProcessorError, Map[Int, Path]] = {
    try {
      val targetWidth = settings.imageMap.baseImageWidth
      val targetHeight = (settings.imageMap.baseImageWidth / imageMap.aspect).toInt

      val image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
      val g = image.createGraphics()
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g.setStroke(new BasicStroke(settings.imageMap.overlayStrokeWidth.toFloat / targetWidth))

      val color = new Color(settings.imageMap.overlayStrokeColor._1.toFloat,
        settings.imageMap.overlayStrokeColor._2.toFloat,
        settings.imageMap.overlayStrokeColor._3.toFloat)

      g.setColor(color)

      logger.debug(s"${color.getRed}, ${color.getGreen}, ${color.getBlue}")

      val result = imageMap.outlines.map {
        case (objectId, outline) =>
          g.setBackground(new Color(0, 0, 0, 0))
          g.clearRect(0, 0, image.getWidth, image.getHeight)

          val currentTransform = g.getTransform()

          g.scale(targetHeight, targetWidth)
          g.draw(outline)

          g.setTransform(currentTransform)

          val unblurred = Files.createTempFile("intake24", ".png")

          ImageIO.write(image, "png", unblurred.toFile())
          val cmd = new ConvertCmd()
          val op = new IMOperation()
          op.addImage(unblurred.toString)
          op.channel("RBGA")
          op.blur(0.0, settings.imageMap.overlayBlurStrength)

          val outputFile = directory.resolve(UUID.randomUUID().toString + ".png")
          op.addImage(outputFile.toString())
          cmd.setSearchPath("/usr/local/bin")
          cmd.run(op)
          //Files.delete(unblurred)
          logger.debug(s"Unblurred path: ${unblurred.toString}")
          logger.debug(s"Generated outline image for object $objectId: ${outputFile.toString}")

          (objectId, outputFile)
      }

      Right(result)
    }
    catch {
      case e: Throwable => Left(ImageProcessorError(e))
    }
  }
}