package uk.ac.ncl.openlab.intake24.services.fooddb.images

import org.im4java.core.{IMOperation, ImageCommand, Info}
import org.im4java.process.ArrayListOutputConsumer
import org.slf4j.LoggerFactory

import java.awt.image.BufferedImage
import java.awt.{BasicStroke, Color, Dimension, RenderingHints}
import java.nio.file.{Files, Path}
import java.util.UUID
import javax.imageio.ImageIO
import javax.inject.{Inject, Singleton}
import scala.collection.JavaConverters._

@Singleton
class ImageProcessorIM @Inject()(val settings: ImageProcessorSettings) extends ImageProcessor {

  val logger = LoggerFactory.getLogger(classOf[ImageProcessorIM])

  private def initConvertCommand(): ImageCommand = {
    val cmd = new ImageCommand(settings.command: _*)

    settings.cmdSearchPath.foreach(cmd.setSearchPath(_))

    cmd
  }

  private val convertCmd = initConvertCommand()

  override def processForSourceThumbnail(sourceImage: Path, thumbnail: Path): Either[ImageProcessorError, Unit] =
    try {

      val op = new IMOperation()

      op.resize(settings.source.thumbnailWidth)
      op.background("white")
      op.gravity("Center")
      op.extent(settings.source.thumbnailWidth, settings.source.thumbnailHeight)
      op.addImage(sourceImage.toString())
      op.addImage(thumbnail.toString())

      logger.debug(s"Invoking ImageMagick for source image thumbnail: ${((convertCmd.getCommand.asScala) ++ (op.getCmdArgs.asScala)).mkString(" ")}")

      convertCmd.run(op)

      Right(())

    } catch {
      case e: Exception => Left(ImageProcessorError(e))
    }

  def processForAsServed(sourceImage: Path, mainImageDest: Path, thumbnailDest: Path): Either[ImageProcessorError, Unit] = {
    try {
      val op = new IMOperation()

      op.resize(settings.asServed.width)
      op.background("white")
      op.gravity("Center")
      op.extent(settings.asServed.width, settings.asServed.height)
      op.addImage(sourceImage.toString())
      op.addImage(mainImageDest.toString())

      logger.debug(s"Invoking ImageMagick for main image: ${((convertCmd.getCommand.asScala) ++ (op.getCmdArgs.asScala)).mkString(" ")}")

      convertCmd.run(op)

      val op2 = new IMOperation()
      op2.resize((settings.asServed.thumbnailWidth / 0.7).toInt)
      op2.gravity("Center");
      op2.addRawArgs("-crop", "70%x80%+0+0")
      op2.addImage(sourceImage.toString())
      op2.addImage(thumbnailDest.toString())

      logger.debug(s"Invoking ImageMagick for thumbnail: ${((convertCmd.getCommand.asScala) ++ (op2.getCmdArgs.asScala)).mkString(" ")}")

      convertCmd.run(op2)

      Right(())

    } catch {
      case e: Exception => Left(ImageProcessorError(e))
    }
  }

  def processForSelectionScreen(sourceImage: Path, dest: Path): Either[ImageProcessorError, Unit] = {
    try {
      val op = new IMOperation()

      op.resize(settings.selection.width)
      op.background("white")
      op.gravity("Center")
      op.extent(settings.selection.width, settings.selection.height)
      op.addImage(sourceImage.toString())
      op.addImage(dest.toString())

      logger.debug(s"Invoking ImageMagick for selection screen image: ${((convertCmd.getCommand.asScala) ++ (op.getCmdArgs.asScala)).mkString(" ")}")

      convertCmd.run(op)

      Right(())

    } catch {
      case e: Exception => Left(ImageProcessorError(e))
    }
  }

  def processForImageMapBase(sourceImage: Path, dest: Path): Either[ImageProcessorError, Unit] = {
    try {
      val op = new IMOperation()

      op.resize(settings.imageMap.baseImageWidth)
      op.background("white")
      op.gravity("Center")
      op.addImage(sourceImage.toString())
      op.addImage(dest.toString())

      logger.debug(s"Invoking ImageMagick for base image: ${((convertCmd.getCommand.asScala) ++ (op.getCmdArgs.asScala)).mkString(" ")}")

      convertCmd.run(op)

      Right(())
    } catch {
      case e: Exception => Left(ImageProcessorError(e))
    }
  }

  def blur(sourcePath: Path, destPath: Path, blurStrength: Double): Unit = {
    // ImageMagick's blur filter is broken -- see http://www.imagemagick.org/Usage/bugs/blur_trans/
    // The bug was fixed in 6.2.5 but got broken again in 6.8.something
    // Still not fixed as of 6.9.6 :(

    // Workaround from this thread (using 100% resize with Gaussian filter):
    // http://www.imagemagick.org/discourse-server/viewtopic.php?f=3&t=24665&sid=4a6c39fd9ae46dc356d68a396a2f3ac9&start=15#p106183
    // Slower than normal blur, but works correctly

    val op = new IMOperation()
    op.addImage(sourcePath.toString)

    op.filter("Gaussian")
    op.define(s"filter:sigma=$blurStrength")
    op.addRawArgs("-resize", "100%")
    op.addImage(destPath.toString)

    convertCmd.run(op)
  }

  def generateImageMapOverlays(imageMap: AWTImageMap, directory: Path): Either[ImageProcessorError, Map[Int, Path]] = {
    try {
      val targetWidth = settings.imageMap.baseImageWidth
      val targetHeight = (settings.imageMap.baseImageWidth / imageMap.aspect).toInt

      val image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
      val g = image.createGraphics()
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g.setStroke(new BasicStroke(settings.imageMap.outlineStrokeWidth.toFloat / targetWidth))

      val color = new Color(settings.imageMap.outlineColor._1.toFloat,
        settings.imageMap.outlineColor._2.toFloat,
        settings.imageMap.outlineColor._3.toFloat)

      g.setColor(color)

      val result = imageMap.outlines.map {
        case (objectId, outline) =>
          g.setBackground(new Color(0, 0, 0, 0))
          g.clearRect(0, 0, image.getWidth, image.getHeight)

          val currentTransform = g.getTransform()

          g.scale(targetWidth, targetWidth)
          g.draw(outline)

          g.setTransform(currentTransform)

          val unblurred = Files.createTempFile("intake24-overlay-", ".png")
          val outputPath = directory.resolve(UUID.randomUUID().toString + ".png")

          ImageIO.write(image, "png", unblurred.toFile())
          blur(unblurred, outputPath, settings.imageMap.outlineBlurStrength)
          Files.delete(unblurred)

          logger.debug(s"Generated outline image for object $objectId: ${outputPath.toString}")

          (objectId, outputPath)
      }

      Right(result)
    }
    catch {
      case e: Exception => Left(ImageProcessorError(e))
    }
  }

  def processForSlidingScale(sourceImage: Path, dest: Path): Either[ImageProcessorError, Dimension] = {
    try {
      val op = new IMOperation()

      op.resize(settings.slidingScale.baseImageWidth)
      op.background("white")
      op.gravity("Center")
      op.format("\"%w %h\"")
      op.write("info:")
      op.addImage(sourceImage.toString())
      op.addImage(dest.toString())

      logger.debug(s"Invoking ImageMagick for sliding scale image: ${((convertCmd.getCommand.asScala) ++ (op.getCmdArgs.asScala)).mkString(" ")}")

      val consumer = new ArrayListOutputConsumer()
      val command = initConvertCommand()
      command.setOutputConsumer(consumer)
      command.run(op)

      val sizeStrings = consumer.getOutput.get(0).split("\\s+")
      val width = Integer.parseInt(sizeStrings(0))
      val height = Integer.parseInt(sizeStrings(1))

      Right(new Dimension(width, height))
    } catch {
      case e: Exception => Left(ImageProcessorError(e))
    }
  }

  def generateSlidingScaleOverlay(outline: AWTOutline, directory: Path): Either[ImageProcessorError, Path] = {
    try {
      val targetWidth = settings.slidingScale.baseImageWidth
      val targetHeight = (settings.slidingScale.baseImageWidth / outline.aspect).toInt

      val image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB)
      val g = image.createGraphics()
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      val color = new Color(settings.slidingScale.fillColor._1.toFloat,
        settings.slidingScale.fillColor._2.toFloat,
        settings.slidingScale.fillColor._3.toFloat)

      g.setPaint(color)


      g.setBackground(new Color(0, 0, 0, 0))
      g.clearRect(0, 0, image.getWidth, image.getHeight)

      val currentTransform = g.getTransform()

      g.scale(targetWidth, targetWidth)
      g.fill(outline.shape)

      g.setTransform(currentTransform)

      val unblurred = Files.createTempFile("intake24-overlay-", ".png")
      ImageIO.write(image, "png", unblurred.toFile())
      val outputPath = directory.resolve(UUID.randomUUID().toString + ".png")
      blur(unblurred, outputPath, settings.slidingScale.blurStrength)
      Files.delete(unblurred)

      logger.debug(s"Generated outline image for sliding scale: ${outputPath.toString}")

      Right(outputPath)
    } catch {
      case e: Exception => Left(ImageProcessorError(e))
    }
  }
}
