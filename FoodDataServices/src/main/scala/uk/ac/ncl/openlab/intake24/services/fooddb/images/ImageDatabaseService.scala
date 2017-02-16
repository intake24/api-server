package uk.ac.ncl.openlab.intake24.services.fooddb.images


import java.time.LocalDateTime

import uk.ac.ncl.openlab.intake24.errors.{DeleteError, LookupError, UnexpectedDatabaseError}

case class SourceImageRecord(id: Long, path: String, thumbnailPath: String, keywords: Seq[String], uploader: String, uploadedAt: LocalDateTime)

case class NewSourceImageRecord(path: String, thumbnailPath: String, keywords: Seq[String], uploader: String)

case class SourceImageRecordUpdate(keywords: Seq[String])

case class ProcessedImageRecord(path: String, sourceId: Long, purpose: ProcessedImagePurpose)

sealed trait ProcessedImagePurpose

object ProcessedImagePurpose {

  case object AsServedMainImage extends ProcessedImagePurpose

  case object AsServedThumbnail extends ProcessedImagePurpose

  case object PortionSizeSelectionImage extends ProcessedImagePurpose

  case object ImageMapBaseImage extends ProcessedImagePurpose

  case object ImageMapOverlay extends ProcessedImagePurpose

  def toId(p: ProcessedImagePurpose) = p match {
    case AsServedMainImage => 1l
    case AsServedThumbnail => 2l
    case PortionSizeSelectionImage => 3l
    case ImageMapBaseImage => 4l
    case ImageMapOverlay => 5l
  }

  def fromId(id: Long) = id match {
    case 1l => AsServedMainImage
    case 2l => AsServedThumbnail
    case 3l => PortionSizeSelectionImage
    case 4l => ImageMapBaseImage
    case 5l => ImageMapOverlay
    case _ => throw new IllegalArgumentException(s"Unexpected processed image purpose value: $id")
  }
}

trait ImageDatabaseService {

  def createSourceImageRecords(records: Seq[NewSourceImageRecord]): Either[UnexpectedDatabaseError, Seq[Long]]

  def getSourceImageRecords(ids: Seq[Long]): Either[LookupError, Seq[SourceImageRecord]]

  def listSourceImageRecords(offset: Int, limit: Int, searchTerm: Option[String]): Either[UnexpectedDatabaseError, Seq[SourceImageRecord]]

  def updateSourceImageRecord(id: Long, update: SourceImageRecordUpdate): Either[LookupError, Unit]

  def deleteSourceImageRecords(ids: Seq[Long]): Either[DeleteError, Unit]

  def createProcessedImageRecords(records: Seq[ProcessedImageRecord]): Either[UnexpectedDatabaseError, Seq[Long]]

  def deleteProcessedImageRecords(ids: Seq[Long]): Either[UnexpectedDatabaseError, Unit]

  def getProcessedImageRecords(ids: Seq[Long]): Either[LookupError, Seq[ProcessedImageRecord]]
}
