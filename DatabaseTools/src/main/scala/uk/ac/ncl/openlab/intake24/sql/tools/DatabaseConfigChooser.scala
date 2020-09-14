package uk.ac.ncl.openlab.intake24.sql.tools

import io.circe.parser.decode
import io.circe.generic.auto._
import java.io.BufferedReader
import java.io.InputStreamReader

object DatabaseConfigChooser {

  val default = IndexedSeq("Development" -> "development-database-config.json", "Test" -> "test-database-config.json", "Production" -> "production-database-config.json")

  def chooseDatabaseConfiguration(configDirPath: String = "./database-config", options: IndexedSeq[(String, String)] = default): DatabaseConfiguration = {

    println()
    println("Please choose the database for this operation:")
    println("=============================================")

    options.zipWithIndex.foreach {
      case ((name, path), index) =>
        println(s"${index + 1}. $name")
    }

    val reader = new BufferedReader(new InputStreamReader(System.in))

    var choice: Option[Int] = None

    while (choice.isEmpty) {
      val input = reader.readLine()

      try {
        val in = input.toInt
        if (in > 0 && in <= options.size)
          choice = Some(in - 1)
        else
          choice = None
      } catch {
        case e: NumberFormatException => choice = None
      }

      if (choice.isEmpty)
        println(s"Please enter a number (1 to ${options.size}) or press Control+C to abort.")
    }

    println()

    decode[DatabaseConfiguration](scala.io.Source.fromFile(configDirPath + "/" + options(choice.get)._2).mkString) match {
      case Right(config) => config
      case Left(error) => throw new RuntimeException(error)
    }
  }
}
