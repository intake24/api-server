package uk.ac.ncl.openlab.intake24.services.fooddb.images

import java.io.File

case class ProcessedAsServedImage(mainImage: File, thumbnail: File)

trait ImageProcessor {

  def processForAsServed(sourceImage: File): Either[ImageProcessorError, ProcessedAsServedImage]
  def processForGuideImageBase(sourceImage: File): Either[ImageProcessorError, File]
  def processForGuideImageOverlays(sourceImage: File): Either[ImageProcessorError, Map[Int, File]]
}
