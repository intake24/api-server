package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.nio.file.Path

trait ImageProcessor {

  def processForSourceThumbnail(sourceImage: Path, thumbnail: Path): Either[ImageProcessorError, Unit]
  def processForAsServed(sourceImage: Path, mainImageDest: Path, thumbnailDest: Path): Either[ImageProcessorError, Unit] 
  def processForSelectionScreen(sourceImage: Path, dest: Path): Either[ImageProcessorError, Unit]
  def processForGuideImageBase(sourceImage: Path, dest: Path): Either[ImageProcessorError, Unit]
  def processForGuideImageOverlays(sourceImage: Path, destDir: Path): Either[ImageProcessorError, Map[Int, Path]]
}
