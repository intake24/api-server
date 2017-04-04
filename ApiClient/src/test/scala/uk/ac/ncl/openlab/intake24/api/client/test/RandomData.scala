package uk.ac.ncl.openlab.intake24.api.client.test

import java.io.{File, FileWriter}

import com.opencsv.CSVWriter
import uk.ac.ncl.openlab
import scala.util.Random

trait RandomData {
/*
  def randomString(length: Int) = Random.alphanumeric.take(length).mkString

  def randomEmail = if (Random.nextBoolean)
    Some(randomString(5) + "@" + randomString(10) + "." + randomString(3))
  else
    None

  def randomName = if (Random.nextBoolean)
    Some(randomString(5) + randomString(5))
  else
    None

  def randomPhone = if (Random.nextBoolean)
    Some(randomString(10))
  else
    None

  def randomCustomFieldNames = Seq.fill(Random.nextInt(5))(randomString(10))

  def randomCustomFieldValues(customFields: Seq[String]): Map[String, String] = customFields.map(k => (k, randomString(10))).toMap

  def randomRoles = Seq.fill(Random.nextInt(5))(randomString(10)).toSet

  def randomPermissions = Seq.fill(Random.nextInt(5))(randomString(10)).toSet


  def randomUserRecordWithPermission(customFieldNames: Seq[String]) =
    UserRecordWithPermissions(randomString(10), randomString(10), randomName, randomEmail, randomPhone, randomCustomFieldValues(customFieldNames), randomRoles, randomPermissions)

  def randomUserRecord(customFieldNames: Seq[String]) =
    UserRecord(randomString(10), randomString(10), randomName, randomEmail, randomPhone, randomCustomFieldValues(customFieldNames))

  private def createTempCSVFile(customFieldNames: Seq[String], records: Seq[UserRecord]): File = {
    val file = File.createTempFile("intake24-", ".csv")
    file.deleteOnExit()

    val writer = new CSVWriter(new FileWriter(file))

    try {
      writer.writeNext(Array("User name", "Password", "Name", "Email", "Phone") ++ customFieldNames)

      records.foreach {
        record =>
          val customFieldValues = customFieldNames.map(record.customFields(_)).toArray
          writer.writeNext(Array(record.userName, record.password, record.name.getOrElse(""), record.email.getOrElse(""), record.phone.getOrElse("")) ++ customFieldValues)
      }
      file
    } catch {
      case e: Throwable => throw e
    } finally {
      writer.close()
    }

  }

  def randomUsersCSV: (Seq[UserRecord], File) = {
    val customFieldNames = randomCustomFieldNames
    val users = Seq.fill(Random.nextInt(9) + 1)(randomUserRecord(customFieldNames))
    (users, createTempCSVFile(customFieldNames, users))
  }*/

}
