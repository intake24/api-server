package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.awt.Dimension
import java.nio.file.Path

trait ImageProcessor {

  def processForSourceThumbnail(sourceImage: Path, thumbnail: Path): Either[ImageProcessorError, Unit]
  def processForAsServed(sourceImage: Path, mainImageDest: Path, thumbnailDest: Path): Either[ImageProcessorError, Unit] 
  def processForSelectionScreen(sourceImage: Path, dest: Path): Either[ImageProcessorError, Unit]
  def processForSlidingScale(sourceImage: Path, dest: Path): Either[ImageProcessorError, Dimension]
  def processForImageMapBase(sourceImage: Path, dest: Path): Either[ImageProcessorError, Unit]
  def generateImageMapOverlays(imageMap: AWTImageMap, directory: Path): Either[ImageProcessorError, Map[Int, Path]]
  def generateSlidingScaleOverlay(outline: AWTOutline, directory: Path): Either[ImageProcessorError, Path]
}
