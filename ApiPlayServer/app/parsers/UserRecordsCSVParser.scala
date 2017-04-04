package parsers

import java.io.{File, FileReader}

import au.com.bytecode.opencsv.CSVReader
import uk.ac.ncl.openlab.intake24.services.systemdb.admin.SurveyUser

import scala.collection.JavaConverters._

object UserRecordsCSVParser {

  val USER_NAME_HEADER = "user name"
  val PASSWORD_HEADER = "password"
  val NAME_HEADER = "name"
  val EMAIL_HEADER = "email"
  val EMAIL_HEADER_ALT = "e-mail"
  val PHONE_HEADER = "phone"

  val fixedColumnNames = Seq(USER_NAME_HEADER, PASSWORD_HEADER, NAME_HEADER, EMAIL_HEADER_ALT, EMAIL_HEADER, PHONE_HEADER)

  private case class HeaderFormat(userNameIndex: Int, passwordIndex: Int, nameIndex: Option[Int], emailIndex: Option[Int], phoneIndex: Option[Int], customFields: Seq[(String, Int)])

  private def toOption(index: Int) = if (index == -1) None else Some(index)

  private val whitespace = "\\s+".r

  private def containsWhitespace(s: String): Boolean = whitespace.findFirstMatchIn(s).isDefined

  private def parseHeader(header: Array[String]): Either[String, HeaderFormat] = {
    if (header.length < 2)
      Left("""Incorrect number of columns in header: at least 2 required ("user name" and "password")""")
    else if (!(header(0).toLowerCase == USER_NAME_HEADER && header(1).toLowerCase == PASSWORD_HEADER))
      Left("""Invalid header: the first two columns must be "user name" and "password" - are you using the correct spreadsheet template?""")
    else {
      val emptyIndex = header.indexWhere(_.trim.isEmpty, 2)

      if (emptyIndex != -1)
        Left(s"""Invalid header: column ${emptyIndex + 1} has no name. Please add a name in the first row or delete the column""")
      else {

        val lowerCase = header.map(_.toLowerCase)

        val userNameIndex = lowerCase.indexWhere(_ == USER_NAME_HEADER)
        val passwordIndex = lowerCase.indexWhere(_ == PASSWORD_HEADER)
        val nameIndex = toOption(lowerCase.indexWhere(_ == NAME_HEADER))
        val emailIndex = toOption(lowerCase.indexWhere(s => s == EMAIL_HEADER || s == EMAIL_HEADER_ALT))
        val phoneIndex = toOption(lowerCase.indexWhere(_ == PHONE_HEADER))

        val customFields = header.zipWithIndex.filter(s => !fixedColumnNames.contains(s._1.toLowerCase))

        Right(HeaderFormat(userNameIndex, passwordIndex, nameIndex, emailIndex, phoneIndex, customFields))
      }
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

  private def parseRow(rowIndex: Int, headerFormat: HeaderFormat, row: Array[String]): Either[String, SurveyUser] =
    if (row.length < 2)
      Left(s"""Too few columns in row ${rowIndex + 1}: at least 2 required, only ${row.length} found""")
    else {
      val userName = row(headerFormat.userNameIndex)
      val password = row(headerFormat.passwordIndex)

      val name = safeGet(row, headerFormat.nameIndex)
      val email = safeGet(row, headerFormat.emailIndex)
      val phone = safeGet(row, headerFormat.phoneIndex)

      val customFields = headerFormat.customFields.foldLeft(Map[String, String]()) {
        case (map, field) => map + (field._1 -> safeGetCustomField(row, field._2))
      }

      if (containsWhitespace(userName) || containsWhitespace(password))
        Left(s"""Spaces and tabs are not allowed in user names or passwords (in row ${rowIndex + 1})""")
      else
        Right(SurveyUser(userName, password, name, email, phone, customFields))
    }

  private def parseRows(headerFormat: HeaderFormat, rows: Seq[Array[String]]): Either[String, Seq[SurveyUser]] = {
    val z: Either[String, Seq[SurveyUser]] = Right(Seq())
    rows.zipWithIndex.foldRight(z) {
      case ((row, rowIndex), acc) =>
        for (parsedRows <- acc.right;
             parsedRow <- parseRow(rowIndex, headerFormat, row).right)
          yield (parsedRow +: parsedRows)
    }
  }

  private def validateUniqueness(userRecords: Seq[SurveyUser]): Either[String, Unit] = {
    val (_, duplicateSet) = userRecords.foldLeft((Set[String](), Set[String]())) {
      case ((unique, duplicate), record) =>
        if (unique.contains(record.userName))
          (unique, duplicate + record.userName)
        else
          (unique + record.userName, duplicate)
    }

    if (duplicateSet.isEmpty)
      Right(())
    else
      Left(s"Duplicate user name(s) found: ${duplicateSet.mkString(", ")}")
  }

  def parseFile(file: File): Either[String, Seq[SurveyUser]] = {
    val reader = new CSVReader(new FileReader(file))

    try {

      val header = reader.readNext()
      val rows = reader.readAll().asScala

      if (rows.size < 1)
        Left("File is empty or in incorrect format")
      else
        for (
          headerFormat <- parseHeader(header).right;
          records <- parseRows(headerFormat, rows).right;
          _ <- validateUniqueness(records).right
        ) yield records
    } catch {
      case e: Throwable => Left(s"""${e.getClass.getSimpleName}: ${e.getMessage}""")
    } finally {
      reader.close()
    }
  }
}
