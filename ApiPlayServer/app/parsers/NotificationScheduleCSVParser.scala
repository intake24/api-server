package parsers

import java.io.{File, FileReader}
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import au.com.bytecode.opencsv.CSVReader
import uk.ac.ncl.openlab.intake24.services.systemdb.notifications.{Notification, RecallNotificationRequest}

import scala.collection.JavaConverters._

object NotificationScheduleCSVParser {

  val EMAIL_HEADER = "email"
  val DATE_TIME_HEADER = "date_time"
  val TYPE_HEADER = "type"

  val fixedColumnNames = Seq(EMAIL_HEADER, DATE_TIME_HEADER, TYPE_HEADER)

  private case class HeaderFormat(emailIndex: Int, dateTimeIndex: Int, typeIndex: Int)

  private val whitespace = "\\s+".r

  private def containsWhitespace(s: String): Boolean = whitespace.findFirstMatchIn(s).isDefined

  private def validNotificationType(s: String): Boolean = Seq(
    Notification.NotificationTypeLoginRecallForYesterday,
    Notification.NotificationTypeLoginRecallForToday,
    Notification.NotificationTypeLoginBackToRecall,
    Notification.NotificationTypeLoginLast
  ).contains(s)

  private def parseHeader(header: Array[String]): Either[String, HeaderFormat] = {
    if (header.length < 3)
      Left("""Incorrect number of columns in header: at least 3 required ("email", "datetime", "type")""")
    else if (!(header(0).toLowerCase == EMAIL_HEADER && header(1).toLowerCase == DATE_TIME_HEADER && header(2).toLowerCase == TYPE_HEADER))
      Left("""Invalid header: header order must be "email", "datetime", "type"""")
    else {
      val emptyEmailIndex = header.indexWhere(_.trim.isEmpty, 0)
      val emptyDatetimeIndex = header.indexWhere(_.trim.isEmpty, 1)
      val emptyTypeIndex = header.indexWhere(_.trim.isEmpty, 2)

      val lowerCase = header.map(_.toLowerCase)

      val emailIndex = lowerCase.indexWhere(_ == EMAIL_HEADER)
      val dateTimeIndex = lowerCase.indexWhere(_ == DATE_TIME_HEADER)
      val typeIndex = lowerCase.indexWhere(_ == TYPE_HEADER)

      Right(HeaderFormat(emailIndex, dateTimeIndex, typeIndex))
    }
  }

  private def safeGet(row: Array[String], index: Option[Int]) = index.flatMap {
    index =>
      if (index < row.length) {
        val colValue = row(index).trim
        if (colValue.nonEmpty)
          Some(colValue)
        else
          None
      }
      else
        None
  }

  private def safeGetCustomField(row: Array[String], index: Int) =
    if (index < row.length)
      row(index)
    else
      ""

  private def parseRow(rowIndex: Int, headerFormat: HeaderFormat, row: Array[String]): Either[String, RecallNotificationRequest] =
    if (row.length < 2)
      Left(s"""Too few columns in row ${rowIndex + 1}: at least 2 required, only ${row.length} found""")
    else {
      val email = row(headerFormat.emailIndex)
      val dateTime = ZonedDateTime.parse(row(headerFormat.dateTimeIndex), DateTimeFormatter.ISO_DATE_TIME)
      val notificationType = row(headerFormat.typeIndex)

      if (containsWhitespace(email))
        Left(s"""Spaces not allowed in user email (in row ${rowIndex + 1})""")
      else if (!validNotificationType(notificationType))
        Left(s"""Invalid notification type: $notificationType (in row ${rowIndex + 1})""")
      else
        Right(RecallNotificationRequest(email, dateTime, notificationType))
    }

  private def parseRows(headerFormat: HeaderFormat, rows: Seq[Array[String]]): Either[String, Seq[RecallNotificationRequest]] = {
    val z: Either[String, Seq[RecallNotificationRequest]] = Right(Seq())
    rows.zipWithIndex.foldRight(z) {
      case ((row, rowIndex), acc) =>
        for (parsedRows <- acc.right;
             parsedRow <- parseRow(rowIndex, headerFormat, row).right)
          yield (parsedRow +: parsedRows)
    }
  }

  def parseFile(file: File): Either[String, Seq[RecallNotificationRequest]] = {
    val reader = new CSVReader(new FileReader(file))

    try {

      val header = reader.readNext()
      val rows = reader.readAll().asScala

      if (rows.size < 1)
        Left("File is empty or in incorrect format")
      else
        for (
          headerFormat <- parseHeader(header).right;
          records <- parseRows(headerFormat, rows).right
        ) yield records
    } catch {
      case e: Throwable => Left(s"""${e.getClass.getSimpleName}: ${e.getMessage}""")
    } finally {
      reader.close()
    }
  }
}
