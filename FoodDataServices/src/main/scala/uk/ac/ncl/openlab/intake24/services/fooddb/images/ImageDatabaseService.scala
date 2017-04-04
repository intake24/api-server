package uk.ac.ncl.openlab.intake24.services.fooddb.images


import java.time.LocalDateTime

import uk.ac.ncl.openlab.intake24.errors.{DeleteError, LookupError, UnexpectedDatabaseError}

case class SourceImageRecord(id: Int, path: String, thumbnailPath: String, keywords: Seq[String], uploader: String, uploadedAt: LocalDateTime)

case class NewSourceImageRecord(path: String, thumbnailPath: String, keywords: Seq[String], uploader: String)

case class SourceImageRecordUpdate(keywords: Seq[String])

case class ProcessedImageRecord(path: String, sourceId: Int, purpose: ProcessedImagePurpose)

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

  def fromId(id: Int) = id match {
    case 1l => AsServedMainImage
    case 2l => AsServedThumbnail
    case 3l => PortionSizeSelectionImage
    case 4l => ImageMapBaseImage
    case 5l => ImageMapOverlay
    case _ => throw new IllegalArgumentException(s"Unexpected processed image purpose value: $id")
  }
}

trait ImageDatabaseService {

  def createSourceImageRecords(records: Seq[NewSourceImageRecord]): Either[UnexpectedDatabaseError, Seq[Int]]

  def getSourceImageRecords(ids: Seq[Int]): Either[LookupError, Seq[SourceImageRecord]]

  def listSourceImageRecords(offset: Int, limit: Int, searchTerm: Option[String]): Either[UnexpectedDatabaseError, Seq[SourceImageRecord]]

  def updateSourceImageRecord(id: Int, update: SourceImageRecordUpdate): Either[LookupError, Unit]

  def deleteSourceImageRecords(ids: Seq[Int]): Either[DeleteError, Unit]

  def createProcessedImageRecords(records: Seq[ProcessedImageRecord]): Either[UnexpectedDatabaseError, Seq[Int]]

  def deleteProcessedImageRecords(ids: Seq[Int]): Either[UnexpectedDatabaseError, Unit]

  def getProcessedImageRecords(ids: Seq[Int]): Either[LookupError, Seq[ProcessedImageRecord]]
}
