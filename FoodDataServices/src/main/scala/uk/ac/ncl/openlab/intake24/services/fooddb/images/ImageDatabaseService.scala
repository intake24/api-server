package uk.ac.ncl.openlab.intake24.services.fooddb.images


import java.time.LocalDateTime

import uk.ac.ncl.openlab.intake24.services.fooddb.errors.{LookupError, UnexpectedDatabaseError}

case class SourceImageRecord(id: Long, path: String, thumbnailPath: String, keywords: Seq[String], uploader: String, uploadedAt: LocalDateTime)

case class NewSourceImageRecord(path: String, thumbnailPath: String, keywords: Seq[String], uploader: String)

case class ProcessedImageRecord(path: String, sourceId: Long, purpose: ProcessedImagePurpose)

sealed trait ProcessedImagePurpose

object ProcessedImagePurpose {
  case object AsServedMainImage extends ProcessedImagePurpose
  case object AsServedThumbnail extends ProcessedImagePurpose
  case object PortionSizeSelectionImage extends ProcessedImagePurpose

  def toId(p: ProcessedImagePurpose) = p match {
    case AsServedMainImage => 1l
    case AsServedThumbnail => 2l
    case PortionSizeSelectionImage => 3l
  }

  def fromId(id: Long) = id match {
    case 1l => AsServedMainImage
    case 2l => AsServedThumbnail
    case 3l => PortionSizeSelectionImage
    case _ => throw new IllegalArgumentException(s"Unexpected processed image purpose value: $id")
  }
}

trait ImageDatabaseService {

  def listSourceImageRecords(offset: Int, limit: Int): Either[UnexpectedDatabaseError, Seq[SourceImageRecord]]
  def createSourceImageRecords(records: Seq[NewSourceImageRecord]): Either[UnexpectedDatabaseError, Seq[Long]]
  def createProcessedImageRecords(records: Seq[ProcessedImageRecord]): Either[UnexpectedDatabaseError, Seq[Long]]
  def deleteProcessedImageRecords(ids: Seq[Long]): Either[UnexpectedDatabaseError, Unit]
  def getSourceImageDescriptors(ids: Seq[Long]): Either[LookupError, Seq[ImageDescriptor]]
  def getProcessedImageRecords(ids: Seq[Long]): Either[LookupError, Seq[ProcessedImageRecord]]
}
