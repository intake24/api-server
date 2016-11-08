package uk.ac.ncl.openlab.intake24.services.fooddb.images


import uk.ac.ncl.openlab.intake24.services.fooddb.errors.LookupError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError
import uk.ac.ncl.openlab.intake24.services.fooddb.errors.UnexpectedDatabaseError

case class SourceImageRecord(path: String, keywords: Seq[String], uploader: String)

case class ProcessedImageRecord(path: String, sourceId: Long, purpose: ProcessedImagePurpose)

sealed trait ProcessedImagePurpose

object ProcessedImagePurpose {
  case object AsServedMainImage extends ProcessedImagePurpose
  case object AsServedThumbnail extends ProcessedImagePurpose
  case object PortionSizeSelectionImage extends ProcessedImagePurpose
}

trait ImageDatabaseService {
  def createSourceImageRecords(records: Seq[SourceImageRecord]): Either[UnexpectedDatabaseError, Seq[Long]]
  def createProcessedImageRecords(records: Seq[ProcessedImageRecord]): Either[UnexpectedDatabaseError, Seq[Long]]
  def deleteProcessedImageRecords(ids: Seq[Long]): Either[UnexpectedDatabaseError, Unit]
  def getSourceImageDescriptors(ids: Seq[Long]): Either[LookupError, Seq[ImageDescriptor]]
}
